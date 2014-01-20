define(['jquery', 'app/authenticator', 'd3/d3', 'd3/d3.layout', 'dagre-d3.min', 'jquery.openid', 'jqueryui/jquery-ui.min', 'jqueryui-editable.min', 'slickgrid/lib/jquery.event.drag-2.2', 'n3', 'jquery.tipsy', 'bootstrap-select.min', 'slickgrid/slick.core', 'slickgrid/slick.formatters', 'slickgrid/slick.grid', 'slickgrid/slick.editors', 'slickgrid/plugins/slick.cellrangedecorator', 'slickgrid/plugins/slick.cellrangeselector', 'slickgrid/plugins/slick.cellselectionmodel', 'loadover'], function($, Authenticator) {"use strict";

	function Application() {
		this.HOST = config.host;
		this.currentVersion = null;

		this.authenticator = new Authenticator();

	};

	function deleteRow() {
		var $tr = $(this).parents('tr');
		if ($tr.data('oldTriple'))
			$('#resource-editor > tbody').data('deletedTriples').push($tr.data('oldTriple'));
		$tr.remove();
	}

	function saveValue(e, params) {
		var $tr = $(this).parents('tr');
		var triple = jQuery.extend(true, {
			p : {
				type : 'uri',
				value : null
			},
			o : {
				type : 'Literal',
				value : null
			}

		}, $tr.data('newTriple') || $tr.data('oldTriple') || {});
		//Copy the new triple, if null copy the old one, if null use default

		triple[$(this).attr('name')].value = params.newValue;

		/*
		 * FUTURE: add comparison between new and old to make sure reverted triples are not deleted!!!!!
		 */
		if ($tr.data('oldTriple'))
			$('#resource-editor > tbody').data('deletedTriples').push($tr.data('oldTriple'));

		$tr.data('newTriple', triple);

	};

	Application.prototype = {
		init : function() {
			var self = this;

			//turn editable into inline mode
			$.fn.editable.defaults.mode = 'inline';

			$('form.openid').openid();
			this.getPROV(function(){
				self.getTypes();
			});

			$('#loginText').on('click', function() {
				self.authenticator.login();
			});

			$('#logoutText').on('click', function() {
				self.authenticator.login();
			});

			$('#editor-load').on('click', function() {
				self.loadResource($('#resource').val());
			});

			$('#editor-save').on('click', function() {
				$('#commit-message-popup').dialog('open');
			});

			$('#editor-add').on('click', function() {
				var $row = $('<tr />');
				$row.append($('<td />').append($('<a href="#" name="p" data-type="textarea" data-pk="1" data-placeholder="Value" data-title="Enter comments" class="editable editable-pre-wrapped editable-click editable-empty" />').text('Empty').editable().on('save', saveValue)));
				$row.append($('<td />').append($('<a href="#" name="o" data-type="textarea" data-pk="1" data-placeholder="Value" data-title="Enter comments" class="editable editable-pre-wrapped editable-click editable-empty" />').text('Empty').editable().on('save', saveValue)));
				var $clear = $('<a />').addClass('glyphicon glyphicon-minus-sign').attr('href', '#').on('click', deleteRow);

				$row.append($('<td />').append($clear));
				$('#resource-editor > tbody').append($row);
			});

			$('#loader').dialog({
				dialogClass : "no-close",
				modal : true,
				autoOpen : false
			});

			$('#commit-message-popup').dialog({
				resizable : false,
				height : 140,
				modal : true,
				autoOpen : false,
				buttons : {
					Submit : function() {
						self.saveResource($(this).find('textarea').val());
						$(this).dialog("close");
					},
					Cancel : function() {
						$(this).dialog("close");
					}
				}
			});

			$('#username').editable({
				url : '/post',
				type : 'text',
				pk : 1,
				name : 'username',
				title : 'Enter username'
			});

			$('#query-language-select').on('change', function() {
				switch ($(this).val()) {
					case 'sparql':
						$('#commit-message').hide();
						break;
					case 'update':
						$('#commit-message').show();
						break;
				}

			});

			$('#query-submit').on('click', function() {

				switch ($('#query-language-select').val()) {
					case 'sparql':
						self.executeSparql($('#query-text').val(), self.buildGrid, function(err) {

						});
						break;
					case 'update':
						self.executeSparqlUpdate($('#query-text').val(), $('#commit-message').val(), function() {
							self.getPROV();
						}, function(err) {

						});

						break;
				}
			});
		},
		getPROV : function(callback) {
			var self = this;
			
			$('#network > .panel-body').loadOverStart();
			
			$.ajax({
				url : this.HOST + 'get',
				beforeSend : function(xhrObj) {
					xhrObj.setRequestHeader("Accept", "text/turtle");
				},
				data : {
					graph : 'urn:rawbase:provenance'
				},
				success : function(data) {
					self.parsePROV(data, function(g, commits) {

						if (!self.currentVersion)
							self.currentVersion = g.nodes()[g.nodes().length -1];
							
						callback();
										
						self.initDagre(g, commits);
					});
				},
				error : function() {

				}
			});
		},
		parseN3 : function(triples, hit, end, error) {
			var parser = new N3.Parser();
			parser.parse(triples, function(err, triple) {
				if (err) {
					error(err);
				} else {
					if (triple) {
						hit(triple);
					} else {
						end();
					}
				}
			});
		},
		parsePROV : function(prov, success, error) {
			var self = this;
			var commits = [];
			// Create a new directed graph
			var g = new dagreD3.Digraph();

			this.parseN3(prov, function(triple) {
				console.log(triple.subject, triple.predicate, triple.object, '.');

				var commit = self.parseCommit(triple, commits[triple.subject]);

				if (commit) {
					commits[commit.iri] = commit;
					if (commit.version) {

						// Add nodes to the graph. The first argument is the node id. The second is
						// metadata about the node. In this case we're going to add labels to each of
						// our nodes.

						if (g.hasNode(commit.version)) {
							g.node(commit.version, {
								//label : commit.version,
								label : '',
								commit : commits[commit.iri]
							});
						} else {
							g.addNode(commit.version, {
								//label : commit.version,
								label : '',
								commit : commits[commit.iri]
							});
						}

					}
				}

				if (triple.predicate == "http://www.w3.org/ns/prov#wasDerivedFrom") {

					// Add edges to the graph. The first argument is the edge id. Here we use null
					// to indicate that an arbitrary edge id can be assigned automatically. The
					// second argument is the source of the edge. The third argument is the target
					// of the edge. The last argument is the edge metadata.
					if (!g.hasNode(triple.object)) {
						g.addNode(triple.object);
					}

					if (!g.hasNode(triple.subject)) {
						g.addNode(triple.subject);
					}
					
					g.addEdge(null, triple.object, triple.subject, {});

				}
			}, function() {
				success(g, commits);
			}, function(err) {

			});
		},
		getTypes : function() {
			var self = this;
			var query = 'SELECT DISTINCT ?type WHERE { ?s a ?type } LIMIT 20';
			
			$('#tab1').loadOverStart();
			
			this.executeSparql(query, function(resultset){
				var results = resultset.results.bindings;
				
				results.forEach(function(result){
					var $li = $('<li class="active" />').appendTo($('#types-list'));

					var $a = $('<a href="#" />')
					.append(result.type.value)
					.appendTo($li);
					
					var countQuery = 'SELECT COUNT(*) AS ?cnt WHERE { ?s a <' + result.type.value + '> }';
					
					self.executeSparql(countQuery, function(data){
						var count = resultset.results.bindings;
						
						$a.append($('<span class="badge pull-right" />').text(count[0].cnt.value));
					});
					
					//Prefetch columns
					var columnQuery = 'SELECT DISTINCT ?p WHERE { ?s a <' + result.type.value + '>; ?p ?o }';
					
					self.executeSparql(countQuery, function(data){
						var predicates = resultset.results.bindings;
						
						$a.data('columns', predicates);
					});

				});
				$('#tab1').loadOverStop();
			},function(error){
				
			});
			
		},
		parseCommit : function(triple, commit) {

			if (!commit)
				commit = {
					iri : triple.subject
				};

			switch (triple.predicate) {
				case 'http://purl.org/dc/terms/title':
					commit.message = triple.object;
					break;
				case 'http://www.w3.org/ns/prov#atTime':
					commit.timestamp = triple.object;
					break;
				case 'http://www.w3.org/ns/prov#generated':
					commit.version = triple.object;
					break;
				case 'http://www.w3.org/ns/prov#wasAssociatedWith':
					commit.author = triple.object;
					break;

				default:
					if (triple.object != 'http://www.w3.org/ns/prov#Activity') {
						return null;
					}
			}

			return commit;
		},
		initDagre : function(g, commits) {
			var self = this;

			var width = $('#graph').width(), height = $('#graph').height();

			//Clear the div
			$('#graph').empty();

			var svg = d3.select("#graph").append("svg").attr("width", width).attr("height", height).append("g").attr("transform", 'translate(20,20)');

			var renderer = new dagreD3.Renderer();
			var layout = dagreD3.layout().nodeSep(5).rankDir("LR");

			var oldDrawNode = renderer.drawNode();
			renderer.drawNode(function(graph, u, svg) {
				oldDrawNode(graph, u, svg);
				$(svg[0]).data('commit', graph.node(u).commit);
				$(svg[0]).data('uri', u);
				//svg.attr("id", graph.node(u).);
			});

			renderer.layout(layout).run(g, svg);

			$('.node rect').attr('x', -5).attr('y', -5).attr('width', 10).attr('height', 10);

			$('.node')
			.each(function(){
				if ($(this).data('uri') === self.currentVersion)
				$(this).attr('class','node-selected');
			})
			.on('click', function() {
				self.currentVersion = $(this).data('uri');
				$('.node-selected').attr('class', "node");
				$(this).attr('class', "node-selected");
			})
			.hover(function() {

				var offset = $(this).offset();
				var width = $(this).outerWidth();

				var commit = $(this).data('commit');
				var $commitDetail = $('#commit-detail');
				
				if (commit.message)
					$commitDetail.find('.graph-message').text(commit.message.split('"')[1]);

				if (commit.iri)
					$commitDetail.find('.graph-hash').text(commit.iri);
				
				if (commit.author)
					self.authenticator.getUser(commit.author, function(user) {
						$commitDetail.find('.graph-photo').html($('<img />').attr('src', user.image.url));
						$commitDetail.find('.graph-name').html($('<a />').attr('href', commit.author).text(user.displayName));
					});
				else {
					$commitDetail.find('.graph-photo').html($('<img />').attr('src', config.defaultAvatar));
					$commitDetail.find('.graph-name').html($('<a />').attr('href', commit.author).text('Anonymous'));
				}
					
				
				if (commit.timestamp)
					$commitDetail.find('.graph-time').text(commit.timestamp.split('"')[1]);

				$('#commit-detail').css({
					top : offset.top,
					left : offset.left + width + 15
				}).show();

			}, function() {
				$('#commit-detail').hide();
			});

			$('#graph > svg').draggable({
				axis : "x"
			});

			$('#graph').height($('#graph > svg').height());
			//$('#graph').width();
			
			$('#network > .panel-body').loadOverStop();

		},
		saveResource : function(message) {
			var self = this;
			var $tbody = $('#resource-editor > tbody');

			function toNTriple(triple) {
				if (triple.o.type == 'uri')
					return '<' + triple.s.value + '> <' + triple.p.value + '> <' + triple.o.value + '> . ';

				return '<' + triple.s.value + '> <' + triple.p.value + '> "' + triple.o.value + '" . ';
			}

			var query = 'INSERT DATA { ';

			$tbody.children('tr').each(function(i, obj) {
				var triple = $(obj).data('newTriple');
				if (triple) {
					if (!triple.p.value || !triple.o.value) {
						self.addErrorMessage('Update is incomplete');
						return;
					}

					triple.s = {
						type : 'uri',
						value : $tbody.data('subject')
					};

					query += toNTriple(triple);
				}
			});

			query += '}; DELETE DATA { ';

			$tbody.data('deletedTriples').forEach(function(triple) {
				triple.s = {
					type : 'uri',
					value : $tbody.data('subject')
				};
				query += toNTriple(triple);

			});

			query += '}';

			this.executeSparqlUpdate(query, message, function(result) {
				self.getPROV();
			}, function(err) {

			});

		},
		loadResource : function(uri) {
			var self = this;
			var query = 'SELECT ?p ?o  WHERE { <' + uri + '> ?p ?o }';

			function processLiteral(l) {
				if (l['xml:lang']) {

					return $('<a href="#" data-type="textarea" data-pk="1" data-placeholder="Value" data-title="Enter comments" class="editable editable-pre-wrapped editable-click" />').text(l.value + '@' + l['xml:lang']);
				} else if (l.datatype) {
					switch (l.datatype) {
						case 'http://www.w3.org/2001/XMLSchema#dateTime':
							return $('<a href="#" data-type="date" data-viewformat="yyyy-mm-dd" data-pk="1" data-placement="right" data-title="When you want vacation to start?" class="editable editable-click"/>').text(l.value);
							break;
					}
				}
				return $('<a href="#" data-type="textarea" data-pk="1" data-placeholder="Value" data-title="Enter comments" class="editable editable-pre-wrapped editable-click" />').text(l.value);
			}

			function processBinding(b) {
				var $a;
				switch (b.type) {
					case 'uri':
						$a = $('<a />').attr('href', b.value).text(b.value);
						break;
					case 'literal':
						$a = processLiteral(b).on('save', saveValue);
						break;
					case 'bnode':
						$a = $('<a />').attr('href', b.value).text(b.value);
						break;
				}
				$a.editable().on('save', saveValue);
				return $a;
			};

			this.executeSparql(query, function(resultset) {
				var results = resultset.results.bindings;

				var $tbody = $('#resource-editor > tbody');

				$tbody.data('deletedTriples', []);

				$tbody.data('subject', uri);

				$tbody.empty();

				for (var i = 0; i < results.length; i++) {
					var $row = $('<tr />');

					var p = results[i].p;
					$('<td />').append(processBinding(p).attr('name', 'p')).appendTo($row);

					var o = results[i].o;
					$('<td />').append(processBinding(o).attr('name', 'o')).appendTo($row);

					var $clear = $('<a />').addClass('glyphicon glyphicon-minus-sign').attr('href', '#').on('click', deleteRow);
					$row.append($('<td />').append($clear));

					$row.data('oldTriple', results[i]);

					$tbody.append($row);
				}
			}, function() {

			});

		},
		executeSparql : function(query, success, error) {
			var self = this;
			var url = this.HOST + "sparql";

			$.ajax({
				url : url,
				beforeSend : function(xhrObj) {
					xhrObj.setRequestHeader("Accept", "application/sparql-results+json");
				},
				data : {
					query : query,
					'rwb-version' : this.currentVersion,
					'rwb-user' : this.user
				},
				success : function(data) {
					success(data);
				},
				error : function(err) {

					self.addErrorMessage(err.statusText);

					error(err);
				}
			});
		},
		addErrorMessage : function(message) {
			$('<div class="alert alert-danger alert-dismissable" />').append('<button type="button" class="close" data-dismiss="alert" aria-hidden="true">&times;</button>').append(message).prependTo($('#results > .panel-body'));
		},
		executeSparqlUpdate : function(query, message, success, error) {
			var self = this;
			var url = this.HOST + "update";

			function execute() {

				var data = {
					'rwb-user' : self.authenticator.getURI(),
					'rwb-message' : message,
					update : query
				};

				if (self.currentVersion)
					data['rwb-version'] = self.currentVersion;

				$('#tab2').loadOverStart();

				$.ajax({
					url : url,
					type : 'POST',
					data : data,
					success : function(data) {
						$('#tab2').loadOverStop();
						success(data);
					},
					error : function(err) {
						$('#tab2').loadOverStop();
						error(err);
					}
				});
			};

			if (!self.authenticator.isAuthenticated()) {
				self.authenticator.login(execute);
			} else {
				execute();
			}

		},
		buildGrid : function(resultset) {
			function requiredFieldValidator(value) {
				if (value == null || value == undefined || !value.length) {
					return {
						valid : false,
						msg : "This is a required field"
					};
				} else {
					return {
						valid : true,
						msg : null
					};
				}
			}

			function URIFormatter(row, cell, value, columnDef, dataContext) {
				return '<a href="' + value + '">' + value + '</a>';
			}

			function buildColumns(head) {
				var columns = [];
				head.vars.forEach(function(c) {
					columns.push({
						id : c,
						name : c,
						field : c,
						minWidth : 120,
						editor : Slick.Editors.LongText
					});
				});

				/* var columns = [
				 {
				 id: "title",
				 name: "Title",
				 field: "title",
				 width: 120,
				 cssClass: "cell-title",
				 editor: Slick.Editors.Text,
				 validator: requiredFieldValidator
				 },

				 {
				 id: "desc",
				 name: "Description",
				 field: "description",
				 width: 100,
				 editor: Slick.Editors.LongText
				 },

				 {
				 id: "duration",
				 name: "Duration",
				 field: "duration",
				 editor: Slick.Editors.Text
				 },

				 {
				 id: "%",
				 name: "% Complete",
				 field: "percentComplete",
				 width: 80,
				 resizable: false,
				 formatter: Slick.Formatters.PercentCompleteBar,
				 editor: Slick.Editors.PercentComplete
				 },

				 {
				 id: "start",
				 name: "Start",
				 field: "start",
				 minWidth: 60,
				 editor: Slick.Editors.Date
				 },

				 {
				 id: "finish",
				 name: "Finish",
				 field: "finish",
				 minWidth: 60,
				 editor: Slick.Editors.Date
				 },

				 {
				 id: "effort-driven",
				 name: "Effort Driven",
				 width: 80,
				 minWidth: 20,
				 maxWidth: 80,
				 cssClass: "cell-effort-driven",
				 field: "effortDriven",
				 formatter: Slick.Formatters.Checkmark,
				 editor: Slick.Editors.Checkbox
				 }
				 ];*/
				return columns;
			}

			var grid;
			var data = [];

			var columns = buildColumns(resultset.head);

			var options = {
				editable : true,
				enableAddRow : true,
				enableCellNavigation : true,
				asyncEditorLoading : true,
				forceFitColumns : true,
				autoEdit : false,
				headerRowHeight : 30
			};

			$(function() {
				var results = resultset.results.bindings;

				for (var i = 0; i < results.length; i++) {
					var item = {};
					for (var key in results[i]) {
						item[key] = results[i][key].value;
					}

					results[i] = item;
				}

				grid = new Slick.Grid("#result-grid", results, columns, options);

				grid.setSelectionModel(new Slick.CellSelectionModel());

				grid.onAddNewRow.subscribe(function(e, args) {
					var item = args.item;
					grid.invalidateRow(data.length);
					data.push(item);
					grid.updateRowCount();
					grid.render();
				});
			});
		}
	};

	return Application;
});


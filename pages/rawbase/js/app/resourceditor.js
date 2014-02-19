define(['jquery'],function($){
	
	function ResourceEditor(){
		
	}
	
	ResourceEditor.prototype = {
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

			function buildRow(property, object) {

				var $tdProperty = $('<td />').append(processUri(property).attr('name', 'p'));
				var $tdValue = $('<td />').append(processObject(object).attr('name', 'o'));

				return $('<tr />').append($tdProperty, $tdValue);
			}

			function processObject(object) {
				return $('<a href="#" id="rdfobject" data-type="rdfobject" data-pk="1" data-title="Please, fill value">').editable({
					value : object,
					validate : function(value) {
						if (value.value == '')
							return 'Value is required!';
					},
					display : function(value) {
						if (!value) {
							$(this).empty();
							return;
						}
						var html = '<b>' + $('<div>').text(value.value).html() + '</b>';
						html += object['xml:lang'] ? '@' + object['xml:lang'] : '';
						html += object['datatype'] ? '^^' + object['datatype'] : '';

						$(this).html(html);
					}
				});
			}

			function processUri(uri) {
				return $('<a />').attr('href', uri.value).text(uri.value).editable();
			}

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

					var $row = buildRow(results[i].p, results[i].o);

					var $clear = $('<a />').addClass('glyphicon glyphicon-minus-sign').attr('href', '#').on('click', deleteRow);
					$row.append($('<td />').append($clear));

					$row.data('oldTriple', results[i]);

					$tbody.append($row);
				}
			}, function() {

			});

		},
	};
	
	return ResourceEditor;
});

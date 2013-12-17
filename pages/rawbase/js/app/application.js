define(['jquery', 'app/authenticator', 'd3/d3', 'd3/d3.layout', 'jquery.openid', 'jqueryui/jquery-ui.min', 'jqueryui-editable.min', 'slickgrid/lib/jquery.event.drag-2.2', 'n3', 'jquery.tipsy', 'bootstrap-select.min', 'slickgrid/slick.core', 'slickgrid/slick.formatters', 'slickgrid/slick.grid', 'slickgrid/slick.editors', 'slickgrid/plugins/slick.cellrangedecorator', 'slickgrid/plugins/slick.cellrangeselector', 'slickgrid/plugins/slick.cellselectionmodel'], function($, Authenticator) {
    "use strict";

    function Application() {
        this.HOST = config.host;
        this.currentVersion = null;

        this.authenticator = new Authenticator();

    };

    Application.prototype = {
        init : function() {
            var self = this;

            $('form.openid').openid();
            this.getPROV();

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
                self.saveResource();
            });

            $('#editor-add').on('click', function() {
                $('#resource-editor > tbody').append($('<tr>' + 
                    '<td><a href="#" name="p" data-type="textarea" data-pk="1" data-placeholder="Value" data-title="Enter comments" class="editable editable-pre-wrapped editable-click" /></td>'
                    +'<td><a href="#" name="o" data-type="textarea" data-pk="1" data-placeholder="Value" data-title="Enter comments" class="editable editable-pre-wrapped editable-click" /></td>'
                    + '</tr>'));
                                
            });

            $('#loader').dialog({
                dialogClass : "no-close",
                modal : true,
                autoOpen : false
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

            function update() {

                self.executeSparqlUpdate($('#query-text').val(), $('#commit-message').val(), function() {
                    self.getPROV();
                }, function(err) {

                    });
            };

            $('#query-submit').on('click', function() {

                switch ($('#query-language-select').val()) {
                    case 'sparql':
                        self.executeSparql($('#query-text').val(), self.buildGrid, function(err) {

                            });
                        break;
                    case 'update':
                        if (!self.authenticator.isAuthenticated()) {
                            self.authenticator.login(update);
                        } else {
                            update();
                        }

                        break;
                }
            });
        },
        getPROV : function() {
            var self = this;
            $.ajax({
                url : this.HOST + 'get',
                beforeSend : function(xhrObj) {
                    xhrObj.setRequestHeader("Accept", "text/turtle");
                },
                data : {
                    graph : 'urn:rawbase:provenance'
                },
                success : function(data) {
                    self.parsePROV(data, function(links, nodes, commits) {
                        self.initD3(null, links, nodes, commits);
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
            var links = [], nodes = {}, commits = {};

            this.parseN3(prov, function(triple) {
                console.log(triple.subject, triple.predicate, triple.object, '.');

                var commit = self.parseCommit(triple, commits[triple.subject]);

                if (commit) {
                    commits[commit.iri] = commit;
                    if (commit.version) {
                        nodes[commit.version] = {
                            name : commit.version,
                            commit : commit.iri
                        };
                    }
                }

                if (triple.predicate == "http://www.w3.org/ns/prov#wasDerivedFrom") {
                    links.push({
                        source : triple.subject,
                        target : triple.object,
                        type : triple.predicate
                    });
                }
            }, function() {
                success(links, nodes, commits);
            }, function(err) {

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
        toggleLoader : function() {
            if (!$('#loader').dialog("isOpen"))
                $('#loader').dialog("open");
            else
                $('#loader').dialog("close");
        },
        initD3 : function(error, links, nodes, commits) {
            var self = this;
            //                var nodes = {};
            //
            // Compute the distinct nodes from the links.
            links.forEach(function(link) {
                link.source = nodes[link.source] || (nodes[link.source] = {
                    name : link.source
                });
                link.target = nodes[link.target] || (nodes[link.target] = {
                    name : link.target
                });
            });

            for (var key in nodes) {
                this.currentVersion = nodes[key].name;
                break;
            }

            var width = $('#graph').width(), height = $('#graph').height();

            //Clear the div
            $('#graph').empty();

            var force = d3.layout.force().nodes(d3.values(nodes)).links(links).size([width, height]).linkDistance(10).charge(-800).on("tick", tick).start();

            var svg = d3.select("#graph").append("svg").attr("width", width).attr("height", height);

            // build the arrow.
            svg.append("svg:defs").selectAll("marker").data(["end"])// Different link/path types can be defined here
            .enter().append("svg:marker")// This section adds in the arrows
            .attr("id", String).attr("viewBox", "0 -5 10 10").attr("refX", 15).attr("refY", -1.5).attr("markerWidth", 6).attr("markerHeight", 6).attr("orient", "auto").append("svg:path").attr("d", "M0,-5L10,0L0,5");

            var link = svg.selectAll(".link").data(force.links()).enter().append("line").attr("class", "link").attr("marker-end", "url(#end)");

            var node = svg.selectAll(".node").data(force.nodes()).enter().append("g").attr("class", function(d) {
                if (self.currentVersion == d.name) {
                    return "node-selected";
                }
                return "node";
            }).on("mouseover", mouseover).on("mouseout", mouseout).on("click", click).call(force.drag);

            node.append("circle").attr("r", function(d) {
                if (self.currentVersion == d.name) {
                    return 8;
                }
                return 4;
            });

            $('svg circle').tipsy({
                gravity : 's',
                html : true,
                title : function() {
                    var commit = commits[this.__data__.commit];

                    var html = '<table><tr><td>Message: </td><td>' + commit.message.split('"')[1] + '</td></tr>';
                    html += '<tr><td>Author: </td><td><a href="' + commit.author + '">' + commit.author + "</a></td></tr>";
                    html += '<tr><td>Time: </td><td>' + commit.timestamp.split('"')[1] + '</td></tr>';

                    return html;
                }
            });

            node.append("text").attr("x", 6).attr("dy", ".35em").text(function(d) {
                return d.name;
            });

            function tick() {
                link.attr("x1", function(d) {
                    return d.source.x;
                }).attr("y1", function(d) {
                    return d.source.y;
                }).attr("x2", function(d) {
                    return d.target.x;
                }).attr("y2", function(d) {
                    return d.target.y;
                });

                node.attr("transform", function(d) {
                    return "translate(" + d.x + "," + d.y + ")";
                });
            }

            function mouseover() {
                d3.select(this).select("circle").transition().duration(200).attr("r", 8);
            }

            function mouseout() {
                if (self.currentVersion != d3.select(this).select("text").text()) {
                    d3.select(this).select("circle").transition().duration(200).attr("r", 4);
                }
            }

            function click() {
                node.selectAll("circle").attr("r", 4);
                node.attr("class", "node");

                d3.select(this).select("circle").attr("r", 8);
                d3.select(this).attr("class", "node-selected");

                self.currentVersion = d3.select(this).select("text").text();

                self.executeSparql($('#query-text').val(), self.buildGrid, function(err) {

                    });
            }

        },
        saveResource : function() {
            var $tbody = $('#resource-editor > tbody');

            function toNTriple(triple) {
                return '<' + triple.s + '> <' + triple.p + '> <' + triple.o + '> .';
            }

            $tbody.children('tr').data('new-triple').each(function(i, obj) {
                console.log(obj);
            });

            $tbody.data('deleted-triples').each(function(i,obj){
                console.log(obj);
            });
        },
        loadResource : function(uri) {
            var self = this;
            var query = 'SELECT  ?p ?o  WHERE { <' + uri + '> ?p ?o }';

            function processLiteral(l) {
                if (l['xml:lang']) {
                    return $('<a href="#" data-type="address" data-pk="1" data-title="Please, fill address" class="editable editable-click" style="display: inline;" />').text(l.value + '@' + l['xml:lang']);
                } else if (l.datatype) {
                    switch (l.datatype) {
                        case 'http://www.w3.org/2001/XMLSchema#dateTime':
                            return $('<a href="#" data-type="date" data-viewformat="yyyy-mm-dd" data-pk="1" data-placement="right" data-title="When you want vacation to start?" class="editable editable-click"/>').text(l.value);
                            break;
                    }
                }
                return $('<a href="#" data-type="textarea" data-pk="1" data-placeholder="Value" data-title="Enter comments" class="editable editable-pre-wrapped editable-click" />').text(l.value);
            }

            function saveValue(e, params) {
                var $tr = $(this).parents('tr');

                if ($tr.data('new-triple')) {
                    var triple = $(this).data('old-triple') || {};
                    triple[$(this).attr('name')] = params.newValue;
                    $tr.data('new-triple', triple);
                }
            };

            function deleteRow() {
                $tr = $(this).parents('tr');
                $tbody.data('deleted-triples').push($tr.data('old-triple'));
                $tr.remove();
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

                $tbody.data('deleted-triples', []);

                $tbody.empty();

                for (var i = 0; i < results.length; i++) {
                    var $row = $('<tr />');

                    var p = results[i].p;
                    $row.append(processBinding(p).attr('name', 'p'));

                    var o = results[i].o;
                    $row.append(processBinding(o).attr('name', 'o'));

                    var $clear = $('<a />').addClass('glyphicon glyphicon-minus-sign').attr('href', '#').on('click', deleteRow);
                    $row.append($('<td />').append($clear));

                    results[i].s = uri;
                    $row.data('old-triple', results[i]);

                    $tbody.append($row);
                }
            }, function() {

                });

        },
        executeSparql : function(query, success, error) {
            var self = this;
            var url = this.HOST + "sparql";
            this.toggleLoader();
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
                    self.toggleLoader();
                    success(data);
                },
                error : function(err) {
                    self.toggleLoader();

                    $('<div class="alert alert-danger alert-dismissable" />').append('<button type="button" class="close" data-dismiss="alert" aria-hidden="true">&times;</button>').append(err.statusText).prependTo($('#results > .panel-body'));

                    error(err);
                }
            });
        },
        executeSparqlUpdate : function(query, message, success, error) {
            var self = this;
            var url = this.HOST + "update";

            var data = {
                'rwb-user' : this.authenticator.getURI(),
                'rwb-message' : message,
                update : query
            };

            if (this.currentVersion)
                data['rwb-version'] = this.currentVersion;

            this.toggleLoader();

            $.ajax({
                url : url,
                type : 'POST',
                data : data,
                success : function(data) {
                    self.toggleLoader();
                    success(data);
                },
                error : function(err) {
                    self.toggleLoader();
                    error(err);
                }
            });
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
                autoEdit : false
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


define( ['jquery',
    'd3/d3',
    'd3/d3.layout',
    'jquery.openid', 
    'jqueryui/jquery-ui.min',
    'slickgrid/lib/jquery.event.drag-2.2',
    'n3',
    'sigma/sigma.min',
    'sigma/sigma.parseGexf',
    'jquery.tipsy',
    'bootstrap-select.min',
    'slickgrid/slick.core', 
    'slickgrid/slick.formatters',
    'slickgrid/slick.grid', 
    'slickgrid/slick.editors', 
    'slickgrid/plugins/slick.cellrangedecorator',
    'slickgrid/plugins/slick.cellrangeselector',
    'slickgrid/plugins/slick.cellselectionmodel'
    ], 
    function( $ , d3) {
        "use strict";
        
        function Application(){
            this.HOST = "http://localhost:3030/rawbase/";
            this.currentVersion = null;
            this.user = null;
        };
        
        Application.prototype = {
            init: function(){
                var self = this;
                
                $('form.openid').openid();
                this.getPROV();
                
                $('#query').find('.btn').on('click',function(){
                    self.executeSparql($('#query-text').val(), 
                        self.buildGrid, 
                        function(err){
                        
                        });
                });
                
            },
            
            getPROV: function(){
                var self = this;
                $.ajax({
                    url: this.HOST + 'get',
                    beforeSend: function(xhrObj){                     
                        xhrObj.setRequestHeader("Accept","text/turtle");                
                    },
                    data: {
                        graph: 'urn:rawbase:provenance' 
                    },
                    success: function(data){
                        self.parsePROV(data, function(links){
                            self.initD3(null, links);
                        });
                    },
                    error: function(){

                    }
                });
            },
            parsePROV: function(prov, success, error){
                
                var self = this;
                var $list = $("#network").find('.selectpicker');
                
                $list.on('change', function(){
                    var $selected = $(this).find(":selected");
                    self.current = $selected.text();
                });
                
                
                var links = [];
                
                var parser = new N3.Parser();
                parser.parse(prov,
                    function (err, triple) {
                        if (err){
                            error(err);
                        } else {
                            if (triple){
                                console.log(triple.subject, triple.predicate, triple.object, '.');
                                        
                                if (triple.predicate == 'http://www.w3.org/ns/prov#generated'){
                                    self.currentVersion = triple.object;
                                
                                    $("<option />")
                                    .appendTo($list)
                                    .text(triple.object);
      
                                }
                                
                                if(triple.predicate == "http://www.w3.org/ns/prov#wasDerivedFrom"){
                                    links.push({
                                        source: triple.subject, 
                                        target: triple.object,
                                        type: triple.predicate
                                    });
                                }
                                
                            }else{
                                console.log("# That's all, folks!");
                                success(links);
                            }
                        }
                    });
                    
            //$list.selectpicker();
            },
            initD3: function(error, links) {
                var self = this;
                var nodes = {};

                // Compute the distinct nodes from the links.
                links.forEach(function(link) {
                    link.source = nodes[link.source] || (nodes[link.source] = {
                        name: link.source
                    });
                    link.target = nodes[link.target] || (nodes[link.target] = {
                        name: link.target
                    });
                });


                var width = $('#graph').width(),
                height = $('#graph').height();

                var force = d3.layout.force()
                .nodes(d3.values(nodes))
                .links(links)
                .size([width, height])
                .linkDistance(50)
                .charge(-300)
                .on("tick", tick)
                .start();

                var svg = d3.select("#graph").append("svg")
                .attr("width", width)
                .attr("height", height);
                
                // build the arrow.
                svg.append("svg:defs").selectAll("marker")
                .data(["end"])      // Different link/path types can be defined here
                .enter().append("svg:marker")    // This section adds in the arrows
                .attr("id", String)
                .attr("viewBox", "0 -5 10 10")
                .attr("refX", 15)
                .attr("refY", -1.5)
                .attr("markerWidth", 6)
                .attr("markerHeight", 6)
                .attr("orient", "auto")
                .append("svg:path")
                .attr("d", "M0,-5L10,0L0,5");

                var link = svg.selectAll(".link")
                .data(force.links())
                .enter().append("line")
                .attr("class", "link")
                .attr("marker-end", "url(#end)");

                var node = svg.selectAll(".node")
                .data(force.nodes())
                .enter().append("g")
                .attr("class", "node")
                .on("mouseover", mouseover)
                .on("mouseout", mouseout)
                .on("click", click)
                .call(force.drag);

                node.append("circle")
                .attr("r", 8);

                node.append("text")
                .attr("x", 12)
                .attr("dy", ".35em")
                .text(function(d) {
                    return d.name;
                });

                function tick() {
                    link
                    .attr("x1", function(d) {
                        return d.source.x;
                    })
                    .attr("y1", function(d) {
                        return d.source.y;
                    })
                    .attr("x2", function(d) {
                        return d.target.x;
                    })
                    .attr("y2", function(d) {
                        return d.target.y;
                    });

                    node
                    .attr("transform", function(d) {
                        return "translate(" + d.x + "," + d.y + ")";
                    });
                }

                function mouseover() {
                    d3.select(this).select("circle").transition()
                    .duration(200)
                    .attr("r", 16);
                }

                function mouseout() {
                    if (self.currentVersion != d3.select(this).select("text").text()){
                        d3.select(this).select("circle").transition()
                        .duration(200)
                        .attr("r", 8);
                    }
                }
                
                function click() {
                    d3.select("circle").attr("r", 8);
                    d3.select(this).select("circle").attr("r", 16);
                    
                    self.currentVersion = d3.select(this).select("text").text();
                }
            },
            executeSparql: function (query, success, error){
                var url = this.HOST + "sparql";
                                
                $.ajax({
                    url: url,
                    beforeSend: function(xhrObj){
                        xhrObj.setRequestHeader("Accept","application/sparql-results+json");              
                    },
                    data: {
                        query: query,
                        'rwb-version': this.currentVersion,
                        'rwb-user': this.user
                    },
                    success: function(data){
                        success(data);
                    },
                    error: function(err){
                        error(err);              
                    }
                });
            },
            buildGrid: function(resultset){
                function requiredFieldValidator(value) {
                    if (value == null || value == undefined || !value.length) {
                        return {
                            valid: false, 
                            msg: "This is a required field"
                        };
                    } else {
                        return {
                            valid: true, 
                            msg: null
                        };
                    }
                }
                
                function URIFormatter(row, cell, value, columnDef, dataContext) {
                    return '<a href="' + value + '">' + value + '</a>';
                }
                
                function buildColumns(head){
                    var columns = [];
                    head.vars.forEach(function(c){
                        columns.push({
                            id: c, 
                            name: c, 
                            field: c, 
                            minWidth: 120,
                            editor: Slick.Editors.LongText
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
                    editable: true,
                    enableAddRow: true,
                    enableCellNavigation: true,
                    asyncEditorLoading: true,
                    autoEdit: false
                };

                $(function () {
                    var results = resultset.results.bindings
                             
                    for (var i = 0; i < results.length; i++) {
                        var item = {};
                        for (var key in results[i]){
                            item[key] = results[i][key].value
                        }
     
                        results[i] = item;
                    }

                    grid = new Slick.Grid("#myGrid", results, columns, options);

                    grid.setSelectionModel(new Slick.CellSelectionModel());

                    grid.onAddNewRow.subscribe(function (e, args) {
                        var item = args.item;
                        grid.invalidateRow(data.length);
                        data.push(item);
                        grid.updateRowCount();
                        grid.render();
                    });
                })
            }

        };
        
        return Application;
    }

    );



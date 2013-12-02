define( ['jquery',
    'jquery.openid', 
    'jqueryui/jquery-ui.min',
    'slickgrid/lib/jquery.event.drag-2.2',
    'n3',
    'sigma/sigma.min',
    'sigma/sigma.parseGexf',
    'bootstrap-select.min',
    'slickgrid/slick.core', 
    'slickgrid/slick.formatters',
    'slickgrid/slick.grid', 
    'slickgrid/slick.editors', 
    'slickgrid/plugins/slick.cellrangedecorator',
    'slickgrid/plugins/slick.cellrangeselector',
    'slickgrid/plugins/slick.cellselectionmodel'
    ], 
    function( $ ) {
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
                        self.parsePROV(data, function(){
                            
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
                
                //GRAPH STUFF!!!
                
                var sigRoot = document.getElementById('graph');
                var sigInst = sigma.init(sigRoot);
                sigInst.addNode('hello',{
                    label: 'Hello',
                    color: '#ff0000'
                }).addNode('world',{
                    label: 'World !',
                    color: '#00ff00'
                }).addEdge('hello_world','hello','world').draw();
                
   
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
                            }else{
                                console.log("# That's all, folks!");
                                success();
                            }
                        }
                    });
                    
            //$list.selectpicker();
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



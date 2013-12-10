var host = "http://localhost:3030/rawbase/"

$(function() { 
        $('form.openid').openid();
        getProvGraph();
        });
                        
var prov = {
        entity: 'http://www.w3.org/ns/prov#Entity',
        generated: 'http://www.w3.org/ns/prov#generated'
}
                        
var current = null;
                        
                        
function getProvGraph(){
        var url = host + "get";
                                
        /*var width = 960,
                                height = 2200;

                                var cluster = d3.layout.cluster()
                                .size([height, width - 160]);

                                var diagonal = d3.svg.diagonal()
                                .projection(function(d) { return [d.y, d.x]; });

                                var svg = d3.select("#network").append("svg")
                                .attr("width", width)
                                .attr("height", height)
                                .append("g")
                                .attr("transform", "translate(40,0)");*/
                                
                                
        $.ajax({
                url: url,
                beforeSend: function(xhrObj){
                                                
                        xhrObj.setRequestHeader("Accept","text/turtle");
                                                
                },
                accept: {},
                data: {
                        graph: 'urn:rawbase:provenance' 
                },
                success: function(data){
                        parseProv(data, getOverview);
                },
                error: function(){
                                                
                }
        });
                                
}
                        
function parseProv(prov, callback){
                               
        var $list = $("<select />").appendTo($("#network"));
                                
        $list.on('change',function(){
                current = $(this).val();
                callback();                        
        })
                                
        var parser = new N3.Parser();
        parser.parse(prov,
                function (error, triple) {
                        if (triple){
                                console.log(triple.subject, triple.predicate, triple.object, '.');
                                        
                                if (triple.predicate == 'http://www.w3.org/ns/prov#generated'){
                                        current = triple.object;
                                        $("<option />")
                                        .appendTo($list)
                                        .val(triple.object)
                                        .text(triple.object);
                                }
                                        
                        }else{
                                console.log("# That's all, folks!");
                                callback();
                        }
                });
                                
                                
                                
}
                        
function getCurrentVersion(){
        return current;
}
                        
function getUser(){
        return "anonymous";
}
                        
function getOverview() {
        var query = 'SELECT ?s FROM <'+getCurrentVersion()+'> WHERE { ?s ?p ?o }';
                                
        var url = host + "sparql";
                                
        $.ajax({
                url: url,
                beforeSend: function(xhrObj){
                        xhrObj.setRequestHeader("Accept","application/sparql-results+json");
                                                
                },
                data: {
                        query: query,
                        'rwb-version': getCurrentVersion(),
                        'rwb-user': getUser()
                },
                success: function(data){
                        $('#properties-table').empty();
                        data.results.bindings.forEach(function(item){
                                console.log(item);
                                $('#properties-table').append('<tr><td><a href="#" onclick="javascript:getResource(this)">' + item.s.value + '</a></td></tr>');
                        });
                },
                error: function(){
                                                
                }
        });
}
                        
function getResource(subject){
        subject = $(subject).text();
                                
        $("#resource-name").text(subject);
                                        
                                        
        var query = 'SELECT ?p ?o FROM <'+getCurrentVersion()+'> WHERE { <' + subject + '> ?p ?o }';
        //var query = 'SELECT ?p ?o FROM <'+getCurrentVersion()+'>WHERE { ?s ?p ?o }';
                                
        var url = host + "sparql";
                                
        $.ajax({
                url: url,
                beforeSend: function(xhrObj){
                        xhrObj.setRequestHeader("Accept","application/sparql-results+json");
                                                
                },
                data: {
                        query: query,
                        'rwb-version': getCurrentVersion(),
                        'rwb-user': getUser()
                },
                success: function(data){
                        $('#properties-table').empty();
                        data.results.bindings.forEach(function(item){
                                $('#properties-table').append('<tr><td><a href="#" onclick="javascript:getResource(this)">' + item.p.value + '</a></td><td>' + item.o.value + '</td></tr>');
                        });
                },
                error: function(){
                                                
                }
        });
}
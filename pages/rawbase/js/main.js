(function (requirejs) {
    "use strict";

    requirejs.config({
        baseUrl: 'rawbase/js/lib',
        paths: {
            jquery: 'jquery-1.10.2.min',
            app: '../app',
            slickgrid: './SlickGrid-2.1',
            jqueryui: './jquery-ui-1.10.3/ui/minified',
            sigma: './sigma',
            d3: './d3'
        },
        shim: {
            'jqueryui/jquery-ui.min': {
                deps: ['jquery']
            },
            'slickgrid/lib/jquery.event.drag-2.2': {
                deps: ['jquery']
            },
            'jquery.openid': {
                deps: ['jquery']
            },
            'jquery.simplemodal.1.4.4.min': {
                deps: ['jquery']  
            },
            'bootstrap.min': {
                deps: ['jquery']
            },
            'bootstrap-select.min': {
                deps: ['bootstrap.min']
            },
            'slickgrid/slick.core': {
                deps: ['jquery','slickgrid/lib/jquery.event.drag-2.2']
            },
            'slickgrid/slick.formatters': {
                deps: ['jquery','slickgrid/slick.core']
            },
            'slickgrid/slick.grid': {
                deps: ['jquery','slickgrid/slick.core']
            },
            'slick.editors': {
                deps: ['jquery','slickgrid/slick.core']
            },
            'slick.cellrangedecorator' : {
                deps:  ['jquery','slickgrid/slick.core'] 
            },
            'slickgrid/plugins/slick.cellrangeselector': {
                deps: ['jquery','slickgrid/slick.core']
            },
            'sigma/sigma.parseGexf': {
                deps: ['sigma/sigma.min']
            },
            'jquery.tipsy' : {
                deps: ['jquery']
            },
            'jqueryui-editable.min' : {
                deps: ['jquery','jqueryui/jquery-ui.min']  
            },
            'n3': {
                exports: 'n3'
            },
            'd3/d3': {
                exports: 'd3'
            },
            'd3/d3.geom': {
                deps: ['d3/d3'],
                exports: 'd3.geom'
            },
            'd3/d3.layout': {
                deps: ['d3/d3.geom'],
                exports: 'd3.layout'
            },
            'dagre-d3.min': {
            	deps: ['d3/d3'],
            	exports: 'dagreD3'
            },
            'x-editable/rdfobject': {
            	deps: ['jqueryui-editable.min']
            }
        },
    });

    require(['app/application', 'bootstrap.min', 'app/config'], function (Application) {
        var app = new Application();
        app.init();
    });
})(requirejs);
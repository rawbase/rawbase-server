(function (requirejs) {
    "use strict";

    requirejs.config({
        baseUrl: 'rawbase/js/lib',
        paths: {
            jquery: 'jquery-1.10.2.min',
            app: '../app',
            slickgrid: './SlickGrid-2.1',
            jqueryui: './jquery-ui-1.10.3/ui/minified',
            sigma: './sigma'
        },
        shim: {
            'jquery-ui.min': {
                deps: ['jquery']
            },
            'SlickGrid-2.1/lib/jquery.event.drag-2.2': {
                deps: ['jquery']
            },
            'jquery.openid': {
                deps: ['jquery']
            },
            'bootstrap.min': {
                deps: ['jquery']
            },
            'bootstrap-select.min': {
                deps: ['bootstrap.min']
            },
            'slick.core': {
                deps: ['jquery','SlickGrid-2.1/lib/jquery.event.drag-2.2']
            },
            'slick.formatters': {
                deps: ['slick.core']
            },
            'slick.grid': {
                deps: ['slick.core']
            },
            'sigma.parseGexf': {
                deps: ['sigma.min']
            },
            'n3': {
                exports: 'n3'
            },
        },
    });

    require(['app/application', 'bootstrap.min'], function (Application) {
        var app = new Application();
        app.init();
    });
})(requirejs);
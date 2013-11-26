(function (requirejs) {
    "use strict";

    requirejs.config({
        shim: {
            'lib/jquery-1.10.2.min': {
                exports: 'jQuery'
            },
            'lib/jquery.openid': {
                deps: ['lib/jquery-1.10.2.min']
            },
            'lib/bootstrap.min': {
                deps: ['lib/jquery-1.10.2.min']
            },
            'lib/n3': {
                exports: 'n3'
            },
        },
    });

    require(['application'], function (Application) {
        var app = new Application();
        app.init();
    });
})(requirejs);
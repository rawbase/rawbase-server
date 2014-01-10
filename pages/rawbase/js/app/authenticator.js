define( ['jquery'], function ($){
    "use strict";
    
    function Authenticator(){
        this.OAUTHURL    =   'https://accounts.google.com/o/oauth2/auth?';
        this.VALIDURL    =   'https://www.googleapis.com/oauth2/v1/tokeninfo?access_token=';
        this.SCOPE       =   'https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/userinfo.email';
        this.CLIENTID    =   config.google.clientid;
        this.REDIRECT    =   config.google.redirect;
        this.LOGOUT      =   'http://accounts.google.com/Logout';
        this.TYPE        =   'token';
        this.KEY		 =   'AIzaSyD3XA9W9Sw5ComQ7be7lEBIS2IcAlkHgpM';
        this._url        =   this.OAUTHURL + 'scope=' + this.SCOPE + '&client_id=' + this.CLIENTID + '&redirect_uri=' + this.REDIRECT + '&response_type=' + this.TYPE;
        this.acToken;
        this.tokenType;
        this.expiresIn;
        this.user;
        this.loggedIn    =   false;
    };
    
    Authenticator.prototype = {
        login: function(callback) {
            var self = this;
            var win  = window.open(this._url, "windowname1", 'width=800, height=600'); 

            //credits: http://www.netlobo.com/url_query_string_javascript.html
            function gup(url, name) {
                name = name.replace(/[\[]/,"\\\[").replace(/[\]]/,"\\\]");
                var regexS = "[\\#&]"+name+"=([^&#]*)";
                var regex = new RegExp( regexS );
                var results = regex.exec( url );
                if( results == null )
                    return "";
                else
                    return results[1];
            };

            var pollTimer   =   window.setInterval(function() { 
                try {
                    console.log(win.document.URL);
                    if (win.document.URL.indexOf(self.REDIRECT) != -1) {
                        window.clearInterval(pollTimer);
                        var url =   win.document.URL;
                        self.acToken =   gup(url, 'access_token');
                        self.tokenType = gup(url, 'token_type');
                        self.expiresIn = gup(url, 'expires_in');
                        win.close();

                        self.validateToken(self.acToken, callback);
                    }
                } catch(e) {
                }
            }, 500);
        },

        validateToken: function(token, callback) {
            var self = this;
            $.ajax({
                url: this.VALIDURL + token,
                data: null,
                success: function(responseText){  
                    self.getUserInfo(callback);
                    self.loggedIn = true;
                    $('#loginText').hide();
                    $('#logoutText').show();
                },  
                dataType: "jsonp"  
            });
        },

        getUserInfo: function(callback) {
            var self = this;
            $.ajax({
                url: 'https://www.googleapis.com/oauth2/v1/userinfo?access_token=' + this.acToken,
                data: null,
                success: function(resp) {
                    self.user    =   resp;
                    console.log(self.user);
                    $('#uName').text('Welcome ' + self.user.name);
                    $('#imgHolder').attr('src', self.user.picture);
                    if (callback)
                        callback(self.user);
                    
                },
                dataType: "jsonp"
            });
        },
        
        startLogoutPolling:function () {
            $('#loginText').show();
            $('#logoutText').hide();
            this.loggedIn = false;
            $('#uName').text('Welcome ');
            $('#imgHolder').attr('src', 'none.jpg');
        },
        
        isAuthenticated: function() {
          return this.user != null;  
        },
        getURI: function() {
            return this.user.link;
        },
        getUser: function (uri, callback) {
            
            function extractID(uri){
                return uri.substr(uri.lastIndexOf('/') + 1);
            }
            
            var url = 'https://www.googleapis.com/plus/v1/people/' + extractID(uri) + '?key=' + this.KEY;
            
            $.getJSON(url,{}, callback);
        }
        
    }
    
    return Authenticator;
});


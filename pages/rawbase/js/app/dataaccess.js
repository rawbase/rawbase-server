define(['jquery'],function($){
	
	function DataAccess(){
		
	};
	
	DataAccess.prototype = {
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
		executeSparql : function(query, success, error, graph) {
			var self = this;
			var url = this.HOST + "sparql";

			$.ajax({
				url : url,
				beforeSend : function(xhrObj) {
					xhrObj.setRequestHeader("Accept", graph ? "text/turtle" : "application/sparql-results+json");
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
		
	};
	
	return DataAccess;
});
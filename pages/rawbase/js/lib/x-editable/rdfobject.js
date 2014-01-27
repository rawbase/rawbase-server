/**
 RDF Object editable input.
 Internally value stored as {value: "Moscow", "xml:lang": "en", datatype: "xsd:String"}

 @class rdfobject
 @extends abstractinput
 @final
 @example
 <a href="#" id="rdfobject" data-type="rdfobject" data-pk="1">awesome</a>
 <script>
 $(function(){
 $('#rdfobject').editable({
 url: '/post',
 title: 'Enter value, datatype and lang #',
 value: {
 value: "Moscow",
 "xml:lang": "en",
 datatype: "xsd:String"
 }
 });
 });
 </script>
 **/
( function($) {"use strict";

		var RDFObject = function(options) {
			this.init('rdfobject', options, RDFObject.defaults);
		};

		//inherit from Abstract input
		$.fn.editableutils.inherit(RDFObject, $.fn.editabletypes.abstractinput);

		$.extend(RDFObject.prototype, {
			/**
			 Renders input from tpl

			 @method render()
			 **/
			render : function() {
				this.$input = this.$tpl.find('input');
			},

			/**
			 Default method to show value in element. Can be overwritten by display option.

			 @method value2html(value, element)
			 **/
			value2html : function(value, element) {
				if (!value) {
					$(element).empty();
					return;
				}
				var html = '<b>' + $('<div>').text(value.value).html() + '</b>';
				html += object['xml:lang'] ? '@' + object['xml:lang'] : '';
				html += object['datatype'] ? '^^' + object['datatype'] : '';

				$(element).html(html);

			},

			/**
			 Gets value from element's html

			 @method html2value(html)
			 **/
			html2value : function(html) {
				/*
				 you may write parsing method to get value by element's html
				 e.g. "Moscow, st. Lenina, bld. 15" => {Value: "Moscow", datatype: "xsd:String", "xml:lang": "en"}
				 but for complex structures it's not recommended.
				 Better set value directly via javascript, e.g.
				 editable({
				 value: {
				 value: "Moscow",
				 datatype: "xsd:String",
				 "xml:lang": "en"
				 }
				 });
				 */
				return null;
			},

			/**
			 Converts value to string.
			 It is used in internal comparing (not for sending to server).

			 @method value2str(value)
			 **/
			value2str : function(value) {
				var str = '';
				if (value) {
					for (var k in value) {
						str = str + k + ':' + value[k] + ';';
					}
				}
				return str;
			},

			/*
			 Converts string to value. Used for reading value from 'data-value' attribute.

			 @method str2value(str)
			 */
			str2value : function(str) {
				/*
				 this is mainly for parsing value defined in data-value attribute.
				 If you will always set value by javascript, no need to overwrite it
				 */
				return str;
			},

			/**
			 Sets value of input.

			 @method value2input(value)
			 @param {mixed} value
			 **/
			value2input : function(value) {
				if (!value) {
					return;
				}
				this.$input.filter('[name="value"]').val(value.value);
				this.$input.filter('[name="datatype"]').val(value.datatype);
				this.$input.filter('[name="lang"]').val(value['xml:lang']);
			},

			/**
			 Returns value of input.

			 @method input2value()
			 **/
			input2value : function() {
				return {
					value : this.$input.filter('[name="value"]').val(),
					datatype : this.$input.filter('[name="datatype"]').val(),
					"xml:lang" : this.$input.filter('[name="lang"]').val()
				};
			},

			/**
			 Activates input: sets focus on the first field.

			 @method activate()
			 **/
			activate : function() {
				this.$input.filter('[name="value"]').focus();
			},

			/**
			 Attaches handler to submit form in case of 'showbuttons=false' mode

			 @method autosubmit()
			 **/
			autosubmit : function() {
				this.$input.keydown(function(e) {
					if (e.which === 13) {
						$(this).closest('form').submit();
					}
				});
			}
		});

		RDFObject.defaults = $.extend({}, $.fn.editabletypes.abstractinput.defaults, {
			tpl : '<div class="editable-rdfobject"><label><span>Value: </span><input type="text" name="value" class="input-small"></label></div>' + '<div class="editable-rdfobject"><label><span>Type: </span><input type="text" name="datatype" class="input-small"></label></div>' + '<div class="editable-rdfobject"><label><span>Language: </span><input type="text" name="lang" class="input-mini"></label></div>',

			inputclass : ''
		});

		$.fn.editabletypes.rdfobject = RDFObject;

	}(window.jQuery)); 
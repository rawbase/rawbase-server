(function( $ ){
  $.fn.loadOverStart = function() {
    if( !this.length )
      return( this );

    if( this.data('loadOver') )
      return( this );

    var loaddiv, loadtimer, loadframe = 0, overlaydiv, obj = this,
  
    _startload = function()
    {
	var append;
	var properties = {};

	if( obj.height() )
	{
		properties['position'] = 'absolute';
		properties['width'] = obj.outerWidth();
		append = $('body');
		properties['top'] = obj.offset().top;
		properties['left'] = obj.offset().left;
		properties['height'] = obj.outerHeight();
	}
	else
	{
        	properties['height']=100;
		properties['position']='relative';
		properties['width'] = '100%';
		append = obj;
		properties['top'] = 0;
		properties['left'] = 0;
	}

      loaddiv	= $('<div class="loadOverOverlay"><div class="loadOver"><div class="loadOverDiv"></div></div></div>');
      loaddiv.css(properties);

      loaddiv.appendTo( append );

      clearInterval( loadtimer );

      loaddiv.show();

      loadtimer = setInterval( _animate_load, 66 );
    },
    _animate_load = function()
    {
      if( !loaddiv.length || !loaddiv.is(':visible') )
      {
        _stopload();
        return;
      }

      loadframe = loadframe+1;
      if( loadframe>11 )
        loadframe = 0;

      loaddiv.find('.loadOverDiv').css('top', (loadframe * -40) + 'px');
    },
    _stopload = function()
    {
      clearInterval( loadtimer );
      loaddiv.remove();
      obj.data('loadOver', false);
      obj.data('stopFunc', undefined);
    };

    _startload();

    this
      .data('stopFunc',_stopload)
      .data('loadOver', true);

    return( this );
  };

  $.fn.loadOverStop = function() {
    if( !this.length )
      return( this );

    var stopfunc = this.data('stopFunc');

    if( !stopfunc )
      return( this );
    
    stopfunc();

    return( this );
};

})( jQuery );

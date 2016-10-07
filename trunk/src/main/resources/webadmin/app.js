Ext.application({
    name: 'WebAdmin',
	requires: ['Ext.container.Viewport'],
	appFolder: 'app',
	
	controllers: ['LoadingController', 'LoginController', 'HomeController'],
	
    launch: function() {
        this.viewport = Ext.create('Ext.container.Viewport', {
			renderTo: Ext.getBody(),
            layout: 'card',
            items: [
				{
					xtype: "login"
				}
            ]
        });
    }
});
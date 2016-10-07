

Ext.define('WebAdmin.view.LeftMenu', {
    extend: 'Ext.panel.Panel',

    requires: [
        'Ext.layout.container.Accordion',
        'Ext.grid.*'
    ],
    xtype: 'leftMenu',


    layout: 'accordion',
    defaults: {
        bodyPadding: 10
    },

    initComponent: function() {
        Ext.apply(this, {
            items: [{
                title: 'System',
                items: [
                    {
                        xtype: "panel",
                        layout: "vbox",
                        border: false,
                        items: [
                            {
                                xtype: "button",
                                text: "Status",
                                textAlign: "left",
                                width: "100%",
                                listeners: {
                                    click: this.onStatusButtonClick
                                }
                            },
                            {
                                xtype: "button",
                                text: "Service",
                                textAlign: "left",
                                width: "100%",
                                listeners: {
                                    click: this.onServiceButtonClick
                                }
                            }
                        ]
                    }
                ]
            }, {
                title: 'Configuration',
                items: [
                    {
                        xtype: "panel",
                        layout: "vbox",
                        border: false,
                        items: [
                            {
                                xtype: "button",
                                text: "Common info",
                                textAlign: "left",
                                width: "100%",
                                listeners: {
                                        click: this.onCommonInfoButtonClick
                                }
                            }
                        ]
                    }
                ]
            }, {
                title: 'Accordion Item 3',
                html: 'Empty'
            }, {
                title: 'Accordion Item 4',
                html: 'Empty'
            }, {
                title: 'Accordion Item 5',
                html: 'Empty'
            }]
        });
        this.callParent();
    },

//    changeRenderer: function(val) {
//        if (val > 0) {
//            return '<span style="color:green;">' + val + '</span>';
//        } else if(val < 0) {
//            return '<span style="color:red;">' + val + '</span>';
//        }
//        return val;
//    },
//
//    pctChangeRenderer: function(val){
//        if (val > 0) {
//            return '<span style="color:green;">' + val + '%</span>';
//        } else if(val < 0) {
//            return '<span style="color:red;">' + val + '%</span>';
//        }
//        return val;
//    }

    onCommonInfoButtonClick : function() {
        console.log("onCommonInfoButtonClick");
    },
    onStatusButtonClick : function() {
        console.log("onStatusButtonClick");
    },
    onServiceButtonClick : function() {
        console.log("onServiceButtonClick");
    }
});
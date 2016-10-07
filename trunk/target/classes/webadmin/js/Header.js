/**
 * Created by ntunam on 4/11/14.
 */
Ext.define("WebAdmin.view.HeaderTitle", {
    extend: "Ext.panel.Panel",
    xtype: "headerTitle",
    border: false,
    bodyStyle:{"background-color":"#3892d3"},
    html: '<h2>Momo Administrator</h2>'
});

Ext.define("WebAdmin.view.CurrentUserPanel", {
    extend: "Ext.panel.Panel",
    xtype: "currentUserPanel",
    border: false,
    layout: {type: 'hbox', pack: 'end'},
    bodyStyle:{"background-color":"#3892d3"},
    color: "white",
    defaults: {
        menu: [
            {
                text: 'Logout',
                listeners: {
                    click: function () {
                        Ext.Ajax.request({
                            url: '/service/logout',
                            params: {
                            },
                            success: function(response){
                                window.location = "login.html";
                            }
                        });
                    }
                }
            }
        ]
    },
    items: [
        {
            xtype: "button",
            text: "Actions"
        }
    ]
});

Ext.define("WebAdmin.view.Header", {
    extend: "Ext.panel.Panel",
    xtype: "topHeader",
    layout: 'column',
    border: false,
    bodyStyle:{"background-color":"#3892d3"},
//    style : {
//        bodyStyle:{"background-color":"#157fcc"},
//    },
    items: [
        {
            columnWidth: 1,
            xtype: "headerTitle"
        },
        {
            width: 150,
            xtype: "currentUserPanel"
        }
    ]
});
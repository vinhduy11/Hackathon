function getTime24(){
    var date = new Date();
    var h= date.getHours();
    var m= date.getMinutes();
    var s= date.getSeconds();
    h = parseInt(h);
    m = parseInt(m);
    s = parseInt(s);
    if(h <10){
        h = "0" + h;
    }
    if(m<10){
        m = "0" + m;
    }
    if(s<10){
        s = "0" + s;
    }
    return (h + ":" + m + ":" + s);
}
function setSelected(name, val){
        var obj = 'select[name^="'+name+'"] option[value="'+val+'"]';
        $(obj).attr("selected","selected");
}

function setCheckbox(id, trueOrFalse){
    $(id).prop('checked', trueOrFalse);
}

function addOpt(id, val, text){
    var jid = "#" + id;
    //$(jid).append("<option value='"+val+"'>"+text+"</option>");

    $(jid).append($("<option></option>")
             .attr("value",val)
             .text(text));
}

function removeOpt(id){
    var jid = "#" + id;
    $(jid).find('option')
         .remove()
         .end();
}
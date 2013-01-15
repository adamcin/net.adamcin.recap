/*
 *
 */
_g.recap = (function() {
    var recapPath = "/apps/recap/content/recap";
    var contextPath = CQURLInfo.contextPath || "";

    var contextReq = _g.$.ajax({
        dataType: "json",
        url: contextPath + recapPath + ".context.json",
        async: false,
        data: {
            ":ck": (new Date().getTime())
        }
    });

    var recapContext = contextReq.status == 200 ? _g.$.parseJSON(contextReq.responseText) : {};

    var refreshAddressBook = function() {
        var url = contextPath + recapContext.addressBookPath + ".list.json";
        _g.$.getJSON(url, function(data) {
            var listEL = _g.$("#address-list");
            listEL.processTemplate(data);
            listEL.listview("refresh");
            listEL.show();
        });
    };

    var initElements = function() {
        var buttonBarEL = _g.$("#addressbook-buttonbar");
        if (buttonBarEL) {
            buttonBarEL.setTemplateElement("addressbook-buttonbar-tpl");
            buttonBarEL.processTemplate();
            buttonBarEL.show();
        }

        _g.$("#address-list").setTemplateElement("address-list-tpl");

        _g.$("#g-recap-addresses-menu").live("pageshow", function() {
            _g.recap.refreshAddressBook();
        });
    };

    return {
        path: recapPath,
        context: recapContext,
        refreshAddressBook: refreshAddressBook,
        initElements: initElements
    };
})();

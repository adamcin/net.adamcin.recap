/*
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <http://unlicense.org/>
 */

/*
 *
 */
_g.recap = (function() {
    var recapPath = "/libs/recap/content/recap";
    var contextPath = "";
    if (typeof CQURLInfo != 'undefined') {
        contextPath = CQURLInfo.contextPath || contextPath;
    } else if (_g.HTTP) {
        contextPath = _g.HTTP.getContextPath() || contextPath;
    }

    var contextReq = _g.$.ajax({
        dataType: "json",
        url: contextPath + recapPath + ".context.json",
        async: false,
        data: {
            ":ck": (new Date().getTime())
        }
    });

    var recapContext = contextReq.status == 200 ? _g.$.parseJSON(contextReq.responseText) : {};
    
    var addressBook = false;
    var quickPaths = "";
    
    var executeSyncToConsole = function(form) {
        _g.$(form).submit();

        _g.$.mobile.changePage(_g.$("#g-recap-console"));
    };

    var reloadAddressBook = function() {
        var url = contextPath + _g.recap.context.addressBookPath + ".list.json";
        
        _g.$.getJSON(url, function(data) {
        	addressBook = data;
            var listEl = _g.$("#address-list");
            
            listEl.setTemplateElement("g-recap-address-book-tpl");
            listEl.processTemplate(data);
            _g.$(":jqmData(role='button')", listEl )
                .not( ".ui-btn, :jqmData(role='none'), :jqmData(role='nojs')" )
                .buttonMarkup();
            listEl.listview("refresh");
            listEl.show();
        });
    };
    
    var getAddressBook = function() {
        return addressBook;
    }

    var deleteAddress = function(address) {
        var data = {":applyTo": address, ":operation": "delete"};
        _g.$.ajax({url: _g.recap.context.addressBookPath, data: data, type: "POST"}).done(function(respData, status, xhr){
            _g.$.mobile.changePage(_g.$('#g-recap-welcome'));
            _g.recap.reloadAddressBook();
        }).fail(function(resp){
                _g.$("<div class='ui-loader ui-overlay-shadow ui-body-e ui-corner-all'><h1>"+ _g.$.mobile.pageLoadErrorMessage +"</h1></div>")
                    .css({ "display": "block", "opacity": 0.96, "top": _g.$(window).scrollTop() + 100 })
                    .appendTo( _g.$.mobile.pageContainer )
                    .delay( 1000 )
                    .fadeOut( 1500, function(){ _g.$(this).remove(); });
            });
    };

    var getQuickPaths = function() {
        return quickPaths;
    }

    var setQuickPaths = function(value) {
        quickPaths = value;
    }

    return {
        path: recapPath,
        context: recapContext,
        executeSyncToConsole: executeSyncToConsole,
        reloadAddressBook: reloadAddressBook,
        getAddressBook: getAddressBook,
        deleteAddress: deleteAddress,
        getQuickPaths: getQuickPaths,
        setQuickPaths: setQuickPaths
    };
})();

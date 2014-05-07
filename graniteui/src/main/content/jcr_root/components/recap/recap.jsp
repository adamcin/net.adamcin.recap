<%@page import="net.adamcin.recap.addressbook.AddressBookConstants"%>
<%@ page import="com.day.cq.widget.HtmlLibraryManager" %>
<%@ page import="net.adamcin.recap.addressbook.AddressBook" %>
<%--
  ~ This is free and unencumbered software released into the public domain.
  ~
  ~ Anyone is free to copy, modify, publish, use, compile, sell, or
  ~ distribute this software, either in source code form or as a compiled
  ~ binary, for any purpose, commercial or non-commercial, and by any
  ~ means.
  ~
  ~ In jurisdictions that recognize copyright laws, the author or authors
  ~ of this software dedicate any and all copyright interest in the
  ~ software to the public domain. We make this dedication for the benefit
  ~ of the public at large and to the detriment of our heirs and
  ~ successors. We intend this dedication to be an overt act of
  ~ relinquishment in perpetuity of all present and future rights to this
  ~ software under copyright law.
  ~
  ~ THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
  ~ EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
  ~ MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
  ~ IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
  ~ OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
  ~ ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
  ~ OTHER DEALINGS IN THE SOFTWARE.
  ~
  ~ For more information, please refer to <http://unlicense.org/>
  --%>

<%--
  Recap Console component.
--%><%
%><%@page session="false" %><%
%><%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.0" %><%
%><sling:defineObjects /><%
%><%
    AddressBook addressBook = resourceResolver.adaptTo(AddressBook.class);
    if (addressBook != null) {
        pageContext.setAttribute("addressBookPath", addressBook.getResource().getPath());
    } else {
        slingResponse.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    }
%><!doctype html>
<html>
<head>
    <meta charset="utf-8">
    <title>Recap | rsync for Adobe CRX!</title>
    <meta name="viewport" content="width=device-width, minimum-scale=1, maximum-scale=1">
    <%
        HtmlLibraryManager htmlMgr = sling.getService(HtmlLibraryManager.class);
        if (htmlMgr != null) {
            htmlMgr.writeIncludes(slingRequest, out, "recap");
            /*
            if (htmlMgr.getLibraries().containsKey("granite.ui.legacy")) {
            }
            */
        }
    %>
    <script>
    function goToPath(uriPath, addressResourcePath) {
        _g.recap.setQuickPaths(uriPath);
    	$('#quick-recap').attr('href', "${request.contextPath}" + addressResourcePath + ".html" + '?<%=AddressBookConstants.RP_PATHS %>=' + uriPath);
    }
    
    function goToAddress(baseHREF, protocol, hostname, port, uriPath) {
        _g.recap.setQuickPaths(uriPath);
    	$('#quick-recap').attr('href', baseHREF + '?<%=AddressBookConstants.PROP_HOSTNAME %>=' + hostname + '&<%=AddressBookConstants.PROP_IS_HTTPS %>=' + protocol + '&<%=AddressBookConstants.PROP_PORT %>=' + port + '&<%=AddressBookConstants.RP_PATHS %>=' + uriPath);
    }
    $(document).on("change", "#uri", function() {
    	var baseHREF = $("#create-address").attr("href");
    	var addressToCreate = $.mobile.path.parseUrl($('#uri').val());
    	if (addressToCreate.domain !== "") {
    		var addressProtocol = addressToCreate.protocol == "https:";
    		var addressHostname = addressToCreate.hostname;
    		var addressPort = addressToCreate.port;
    		var addressPath = addressToCreate.pathname;
    		var addressHash = addressToCreate.hash;
    		var parsedPath;
    		if (!addressPort) {
    			if (addressToCreate.protocol == "http:") {
    				addressPort = "80";
    			} else if (addressToCreate.protocol == "https:"){
    				addressPort = "443";
    			}
    				
    		}
    		if (addressHash.match('^#/')) {
    			parsedPath = addressHash.substr(1); // check arguments
    		} else {
    			parsedPath = addressPath;
    		}
    		parsedPath = parsedPath.replace(/[.?#].*$/, "")
    		
    		var matched = false;
    		var addressResourcePath;
    		if (_g.recap.getAddressBook()) {
    			var addressBook = _g.recap.getAddressBook();
	    		for (var addressKey in addressBook) {
	    			if (addressBook.hasOwnProperty(addressKey)) {
	    				var address = addressBook[addressKey];
	    				if (address.connectionKey && address.connectionKey == addressToCreate.protocol + "//"+ addressHostname + ":" + addressPort) {
		    				matched = true;
		    				addressResourcePath = address.path;
		    				break;
	    				}
	    			} 
	    		}   
    		}	
    		if (matched) {
    			goToPath(parsedPath, addressResourcePath);
    		} else {
				goToAddress(baseHREF, addressProtocol, addressHostname, addressPort, parsedPath)
			}
    	}
    	else {alert("Please enter a valid URI!");}
	});
	</script>
    
</head>
<body>
    <div class="ui-recapheader" data-role="globalheader" data-title="Recap" data-theme="a"></div>

    <div data-role="panel" data-id="menu" id="g-recap-menu">
        <div data-role="page" id="g-recap-address-book">
            <div data-role="header">
                <h1 class="g-uppercase">Address Book</h1>
            </div>

            <div data-role="content" data-scroll="y" data-theme="c">
                <ul id="address-list" data-role="listview" data-split-theme="c" data-split-icon="gear" style="display:none;"></ul>
            </div>

        </div>
    </div>
	
    <textarea id="g-recap-address-book-tpl" style="display:none;">
		<div class="ui-grid-a" >
			<div class="ui-block-a" data-role="fieldcontain" style="width:70%;">
				<label for="uri" class="ui-hidden-accessible">Quick Recap URI</label>
				<input class="ui-input-text ui-body-d ui-corner-all ui-shadow-inset" name="uri" data-theme="a" type="text" id="uri" placeholder="Paste URI" style="line-height: 37px; margin: 6px 2px 7px; padding: 0px 0px 2px 10px;" />
	    	</div>
	    	<div class="ui-block-b" style="width:30%;">
		    	<a x-cq-linkchecker="skip" data-role="button" data-theme="a" data-panel="main" id="quick-recap" href="${request.contextPath}{_g.recap.context.addressBookPath}/*.edit.html">
			    	Quick Recap
			    </a>
			</div>
	    </div>
	    <li data-role="list-divider"></li>
        {#foreach $T as address}
        <li>
            <a x-cq-linkchecker="skip" href="${request.contextPath}{$T.address.path}.html" data-panel="main">
                <h3>{$T.address.title}</h3>
                <p>{$T.address.url}</p>
            </a>
            <a x-cq-linkchecker="skip" href="${request.contextPath}{$T.address.path}.edit.html" data-panel="main">Edit</a>
        </li>
        {#/for}
        <li style="background:transparent;border-color:transparent;position: absolute; bottom: 0; width: 100%;;">
            <a x-cq-linkchecker="skip" data-role="button" data-theme="a" data-panel="main" id="create-address" href="${request.contextPath}{_g.recap.context.addressBookPath}/*.edit.html">
                Create Address
            </a>
        </li>
    </textarea>

    <div data-role="panel" data-id="main" id="g-recap-main">
        <div data-role="page" id="g-recap-welcome">
            <div data-role="header">
                <h2 class="g-uppercase">Welcome to Recap!</h2>
            </div>

            <div data-role="content">
                <span class="g-big">Recap - rsync for Adobe CRX!</span>

                <p>Recap is based on the 'vlt rcp' command, but focuses on providing a simple web interface for syncing content between CRX instances, using a browser or a command-line tool like curl.</p>

                <h3>Getting Started</h3>

                <ol>
                    <li>Create an address for a remote CRX instance (be sure to specify appropriate credentials with read permission for the content you want to sync)</li>
                    <li>Specify a path that exists on the remote instance to be synced to your local (I do not recommend specifying "/" or any of its children so that it has a chance to complete before the end of the week)</li>
                    <li>Click the 'Start Sync' button.</li>
                </ol>

                <p>It's that easy!</p>

            </div>
        </div>

        <div data-role="page" id="g-recap-console" data-title="Sync Console">
            <div data-role="header">
                <h1 class="g-uppercase">Sync Console</h1>
            </div>

            <div data-role="content" style="overflow:hidden">
                <iframe name="console_frame" width="100%" height="100%"></iframe>
            </div>
        </div>
    </div>

    <script type="text/javascript">
        (function() {
            _g.recap.reloadAddressBook();
        })();
    </script>
</body>
</html>
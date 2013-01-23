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
        <li style="background:transparent;border-color:transparent;">
            <a x-cq-linkchecker="skip" data-role="button" data-theme="a" data-panel="main" href="${request.contextPath}{_g.recap.context.addressBookPath}/*.edit.html">
                Create Address
            </a>
        </li>
        {#foreach $T as address}
        <li>
            <a x-cq-linkchecker="skip" href="${request.contextPath}{$T.address.path}.html" data-panel="main">
                <h3>{$T.address.title}</h3>
                <p>{$T.address.url}</p>
            </a>
            <a x-cq-linkchecker="skip" href="${request.contextPath}{$T.address.path}.edit.html" data-panel="main">Edit</a>
        </li>
        {#/for}
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

            <div data-role="content">
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
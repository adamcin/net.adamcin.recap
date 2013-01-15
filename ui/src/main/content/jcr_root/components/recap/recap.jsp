<%@ page import="com.day.cq.widget.HtmlLibraryManager" %><%--
  Recap Console component.
--%><%
%><%@page session="false" %><%
%><%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.0" %><%
%><sling:defineObjects /><%
%><%
%><!doctype html>
<html>
<head>
    <meta charset="utf-8">
    <title>Recap for Adobe CRX</title>
    <meta name="viewport" content="width=device-width, minimum-scale=1, maximum-scale=1">
    <%
        HtmlLibraryManager htmlMgr = sling.getService(HtmlLibraryManager.class);
        if (htmlMgr != null) {
            htmlMgr.writeIncludes(slingRequest, out, "recap");
        }
    %>
</head>
<body>
    <div data-role="globalheader" data-title="Recap" data-theme="a"></div>

    <div data-role="panel" data-id="menu">
        <div id="g-recap-addresses-menu" data-role="page">
            <div data-role="header">
                <h1 class="g-uppercase">Address Book</h1>
            </div>

            <div data-role="content" data-scroll="y" data-theme="c">
                <ul id="address-list" data-role="listview" style="display:none;"></ul>
            </div>

            <textarea id="address-list-tpl" style="display:none;">
                {#foreach $T as a}
                <li data-icon="false">
                    <a x-cq-linkchecker="skip" href="<%=request.getContextPath()%>{$T.a$key}.edit.html" data-panel="main">
                        <h3>{$T.a.title}</h3>
                        <p>{$T.a.url}</p>
                    </a>
                </li>
                {#/for}
            </textarea>

            <div data-role="footer">
                <div class="g-buttonbar" id="addressbook-buttonbar"></div>
            </div>

            <textarea id="addressbook-buttonbar-tpl" style="display:none;">
                <a id="g-recap-addresses-new" x-cq-linkchecker="skip" href="<%=request.getContextPath()%>{_g.recap.context.addressBookPath}/*.edit.html" data-role="button" data-panel="main">New Address</a>
            </textarea>
        </div>

    </div>

    <div data-role="panel" data-id="main">
        <div data-role="page" id="g-recap-welcome-main">
            <div data-role="header"></div>

            <div data-role="content">
                <span class="g-big">Welcome to Recap</span>
            </div>
        </div>
    </div>

    <script type="text/javascript">
        (function() {
            _g.recap.initElements();
        })();
    </script>
</body>
</html>
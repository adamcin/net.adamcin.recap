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
        <div id="g-recap-favorites-menu" data-role="page">
            <div data-role="header">
                <h1 class="g-uppercase">Sources</h1>
            </div>

            <div data-role="content" data-scroll="y" data-theme="c">
                <ul id="node-list" data-role="listview" style="display:none;"></ul>
            </div>
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
</body>
</html>
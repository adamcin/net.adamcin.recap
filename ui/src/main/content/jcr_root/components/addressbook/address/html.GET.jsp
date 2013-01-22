<%@ page import="com.day.cq.widget.HtmlLibraryManager" %>
<%@ page import="net.adamcin.recap.addressbook.Address" %>
<%@ page import="net.adamcin.recap.addressbook.AddressBookConstants" %>
<%@ page import="net.adamcin.recap.api.Recap" %>
<%@ page import="org.apache.commons.lang.StringEscapeUtils" %>
<%@ page import="org.apache.sling.api.resource.ResourceUtil" %>
<%@ page import="java.util.HashMap" %>
<%--
  Recap Console component.
--%><%
%><%@page session="false" %><%
%><%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.0" %><%
%><sling:defineObjects /><%
%><%
    Recap recap = sling.getService(Recap.class);
    if (recap != null) {
        pageContext.setAttribute("defaultPort", recap.getDefaultPort());
        pageContext.setAttribute("defaultUsername", recap.getDefaultUsername());
        pageContext.setAttribute("defaultPassword", recap.getDefaultPassword());
        pageContext.setAttribute("defaultContextPath", recap.getDefaultContextPath());
    }

    Address address = resource.adaptTo(Address.class);
    String title = "Edit Address";

    if (ResourceUtil.isStarResource(resource)) {
        title = "New Address";
    }

    if (address != null) {
        pageContext.setAttribute("address", address);
    } else {
        pageContext.setAttribute("address", new HashMap<String, Object>());
    }
%><!doctype html>
<html>
<head>
    <meta charset="utf-8">
    <title>Recap for Adobe CRX | <%=title%></title>
    <meta name="viewport" content="width=device-width, minimum-scale=1, maximum-scale=1">
    <%
        HtmlLibraryManager htmlMgr = sling.getService(HtmlLibraryManager.class);
        if (htmlMgr != null) {
            htmlMgr.writeIncludes(slingRequest, out, "recap");
        }
    %>
</head>
<body>
    <div data-role="page" id="g-recap-address-copy-form" data-url="<%=request.getRequestURI()%>">

        <div data-role="header" data-backbtn="false">
            <h1><%=title%></h1>
        </div>

        <%--
        <script type="text/javascript">
            _g.$.mobile.loadPage()
        </script>
        --%>

        <div data-role="content">
            <%
                String error = request.getParameter("error");
                if (error != null && error.trim().length() > 0) { %>
            <p class="error"><%=StringEscapeUtils.escapeHtml(request.getParameter("error"))%></p>
            <% } %>

            <form action="<%=request.getContextPath() + resource.getPath()%>.graniteconsole.html" target="console_frame" method="post">
                  <%--onsubmit="_g.$.mobile.changePage(_g.$('g-recap-welcome-main'), {pageContainer:_g.$('g-recap-main')})">--%>
                <div data-role="fieldcontain">
                    <label for="g-recap-address-copy-paths">Paths *</label>
                    <textarea id="g-recap-address-copy-paths" type="text" required="required" placeholder="Specify root paths to copy, one per line"
                           name="<%=AddressBookConstants.RP_PATHS%>" cols="40" rows="8"></textarea>
                </div>
                <button type="button" onclick="_g.recap.executeSyncToConsole(this.form)" value="Start Copy Session" data-theme="a"></button>
            </form>


        </div>

    </div>

</body>
</html>
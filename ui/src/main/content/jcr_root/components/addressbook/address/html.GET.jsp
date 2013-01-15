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
    <div data-role="page" id="g-recap-address-edit" data-url="<%=request.getRequestURI()%>">

        <div data-role="header" data-backbtn="false">
            <h1><%=title%></h1>
        </div>

        <div data-role="content">
            <%
                String error = request.getParameter("error");
                if (error != null && error.trim().length() > 0) { %>
            <p class="error"><%=StringEscapeUtils.escapeHtml(request.getParameter("error"))%></p>
            <% } %>

            <form action="<%=request.getContextPath() + resource.getPath() + "." + AddressBookConstants.SELECTOR_COPY%>.html" method="post">
                <input id="g-recap-address-edit-resourceType" name="./sling:resourceType" value="<%=AddressBookConstants.RT_ADDRESS%>" type="hidden"/>
                <div data-role="fieldcontain">
                    <label for="g-recap-address-edit-title">Hostname *</label>
                    <input id="g-recap-address-edit-title" type="text" required="required"
                           name="./jcr:title"
                           value="${address.hostname}"/>
                </div>
                <div data-role="fieldcontain">
                    <label for="g-recap-address-edit-hostname">Hostname *</label>
                    <input id="g-recap-address-edit-hostname" type="text" required="required"
                           name="./<%=AddressBookConstants.PROP_HOSTNAME%>"
                           value="${address.hostname}"/>
                </div>
                <div data-role="fieldcontain">
                    <label for="g-recap-address-edit-port">Port</label>
                    <input id="g-recap-address-edit-port" type="text" placeholder="default: ${defaultPort}"
                           name="./<%=AddressBookConstants.PROP_PORT%>"
                           value="${address.port}"/>
                </div>
                <div data-role="fieldcontain">
                    <label for="g-recap-address-edit-username">Username</label>
                    <input id="g-recap-address-edit-username" type="text" placeholder="default: ${defaultUsername}"
                           name="./<%=AddressBookConstants.PROP_USERNAME%>"
                           value="${address.username}"/>
                </div>
                <div data-role="fieldcontain">
                    <label for="g-recap-address-edit-password">Password</label>
                    <input id="g-recap-address-edit-password" type="text" placeholder="default: ${defaultPassword}"
                           name="./<%=AddressBookConstants.PROP_PASSWORD%>"
                           value="${address.password}"/>
                </div>
                <div data-role="fieldcontain">
                    <label for="g-recap-address-edit-contextPath">Servlet Context Path</label>
                    <input id="g-recap-address-edit-contextPath" type="text" placeholder="default: ${defaultContextPath}"
                           name="./<%=AddressBookConstants.PROP_CONTEXT_PATH%>"
                           value="${address.contextPath}"/>
                </div>
                <input id="g-recap-address-edit-isHttpsDelete" name="./<%=AddressBookConstants.PROP_IS_HTTPS%>@Delete" value="true" type="hidden"/>
                <input id="g-recap-address-edit-isHttpsTypeHint" name="./<%=AddressBookConstants.PROP_IS_HTTPS%>@TypeHint" value="Boolean" type="hidden"/>
                <div data-role="fieldcontain">
                    <fieldset data-role="controlgroup">
                        <legend>Protocol</legend>
                        <label for="g-recap-address-edit-isHttp">HTTP</label>
                        <input id="g-recap-address-edit-isHttp" type="radio" value="false"
                               <% if (address == null || Boolean.FALSE.equals(address.isHttps())) { %>checked="true"<% } %>
                               name="./<%=AddressBookConstants.PROP_IS_HTTPS%>" />
                        <label for="g-recap-address-edit-isHttps">HTTPS</label>
                        <input id="g-recap-address-edit-isHttps" type="radio" value="true"
                               <% if (address != null && Boolean.TRUE.equals(address.isHttps())) { %>checked="true"<% } %>
                               name="./<%=AddressBookConstants.PROP_IS_HTTPS%>" />
                    </fieldset>
                </div>
                <input type="submit" value="Save" data-theme="a"/>
            </form>


        </div>

    </div>

</body>
</html>
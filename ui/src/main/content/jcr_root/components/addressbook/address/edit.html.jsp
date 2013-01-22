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

    String name = resource.getName();
    if (ResourceUtil.isStarResource(resource)) {
        title = "New Address";
        name = "star";
    } else {
        pageContext.setAttribute("isEdit", Boolean.TRUE);
    }

    String pageId = "g-recap-address-edit-" + name;

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
    <div data-role="page" id="<%=pageId%>" data-url="<%=request.getRequestURI()%>">

        <div data-role="header" data-backbtn="false">
            <h1><%=title%></h1>
        </div>

        <div data-role="content">
            <%
                String error = request.getParameter("error");
                if (error != null && error.trim().length() > 0) { %>
            <p class="error"><%=StringEscapeUtils.escapeHtml(request.getParameter("error"))%></p>
            <% } %>

            <form action="${request.contextPath}${resource.path}" method="post">
                <input type="hidden" name="_charset_" value="utf-8" />
                <input id="g-recap-address-edit-resourceType" name="./sling:resourceType" value="<%=AddressBookConstants.RT_ADDRESS%>" type="hidden"/>
                <div data-role="fieldcontain">
                    <label for="g-recap-address-edit-title">Address Title *</label>
                    <input id="g-recap-address-edit-title" type="text" required="required"
                           name="./jcr:title"
                           value="${address.title}"/>
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
                    <label for="g-recap-address-edit-contextPath">Servlet Context Path</label>
                    <input id="g-recap-address-edit-contextPath" type="text" placeholder="default: ${defaultContextPath}"
                           name="./<%=AddressBookConstants.PROP_CONTEXT_PATH%>"
                           value="${address.contextPath}"/>
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
            </form>
        </div>

        <div data-role="footer">
            <div class="g-buttonbar">
                <% if (!ResourceUtil.isStarResource(resource)) { %>
                <a class="delete ui-btn-right" href="#" id="g-recap-address-delete" onclick="_g.recap.deleteAddress('${resource.path}')" data-icon="delete" data-iconpos="notext">Delete</a>
                <% } %>
                <a class="done ui-btn-right" href="#" data-icon="save" data-iconpos="notext">Save</a>
            </div>
        </div>

        <%--
        <% if (!ResourceUtil.isStarResource(resource)) { %>
        <div data-role="confirm" data-for="g-recap-address-delete">
            <a data-icon="check" data-role="button">Confirm</a>
            <a data-icon="back" data-role="button">Cancel</a>
        </div>
        <% } %>
        --%>

        <%--
        <c:if test="${isEdit}">
            <div data-role="confirm" data-for="g-recap-address-delete">
                <a data-icon="check" data-role="button">Confirm</a>
                <a data-icon="back" data-role="button">Cancel</a>
            </div>
        </c:if>
        --%>

        <script type="text/javascript">
            _g.$('#<%=pageId%> .done').click(function() {
                var form = _g.$('#<%=pageId%> form');
                var action = form.attr("action");
                var data = form.serialize();
                _g.$.ajax({url: action, data: data, type: "POST"}).done(function(respData, status, xhr){
                    if (xhr.status == 201) {
                        _g.$.mobile.changePage(xhr.getResponseHeader("location")+".edit.html", {pageContainer:_g.$('#g-recap-main')});
                    }
                    _g.recap.reloadAddressBook();
                }).fail(function(resp){
                    _g.$("<div class='ui-loader ui-overlay-shadow ui-body-e ui-corner-all'><h1>"+ _g.$.mobile.pageLoadErrorMessage +"</h1></div>")
                    .css({ "display": "block", "opacity": 0.96, "top": _g.$(window).scrollTop() + 100 })
                    .appendTo( _g.$.mobile.pageContainer )
                    .delay( 1000 )
                    .fadeOut( 1500, function(){ _g.$(this).remove(); });
                });
            });
        </script>

    </div>

</body>
</html>
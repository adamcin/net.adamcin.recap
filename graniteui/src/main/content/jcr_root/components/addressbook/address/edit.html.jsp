<%@ page import="net.adamcin.recap.addressbook.Address" %>
<%@ page import="net.adamcin.recap.addressbook.AddressBookConstants" %>
<%@ page import="net.adamcin.recap.api.Recap" %>
<%@ page import="org.apache.sling.api.resource.ResourceUtil" %>
<%@ page import="java.util.HashMap" %>
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
    Recap recap = sling.getService(Recap.class);
    if (recap != null) {
        String defaultPassword = (recap.getDefaultPassword() != null ? recap.getDefaultPassword() : "");
        pageContext.setAttribute("defaultPort", recap.getDefaultPort());
        pageContext.setAttribute("defaultUsername", recap.getDefaultUsername());
        pageContext.setAttribute("defaultPassword", defaultPassword.replaceAll(".", "*"));
        pageContext.setAttribute("defaultServletPath", recap.getDefaultServletPath());
    }

    Address address = slingRequest.adaptTo(Address.class);
    String title = "Edit Address";

    String name = resource.getName();
    if (ResourceUtil.isStarResource(resource)) {
        title = "New Address";
        name = "star";
    } else {
        pageContext.setAttribute("isEdit", Boolean.TRUE);
    }

    pageContext.setAttribute("title", title);
    String pageId = "g-recap-address-edit-" + name;
    pageContext.setAttribute("pageId", pageId);

    if (address != null) {
    	pageContext.setAttribute("address", address);
    } else {
        pageContext.setAttribute("address", new HashMap<String, Object>());
    }

%><!doctype html>
<html>
<head>
    <meta charset="utf-8">
    <title>Recap | rsync for Adobe CRX!</title>
    <meta name="viewport" content="width=device-width, minimum-scale=1, maximum-scale=1">
</head>
<body>

<div data-role="page" id="${pageId}" data-url="${slingRequest.requestURI}" data-title="${title}">

    <div data-role="header">
        <h1 class="g-uppercase">${title}</h1>
    </div>

    <div data-role="content">
        <form action="${request.contextPath}${resource.path}" method="post">
            <input type="hidden" name="_charset_" value="utf-8" />
            <input id="${pageId}-resourceType" name="./sling:resourceType" value="<%=AddressBookConstants.RT_ADDRESS%>" type="hidden"/>
            <div data-role="fieldcontain">
                <label for="${pageId}-title">Address Title *</label>
                <input id="${pageId}-title" type="text" required="required"
                       name="./jcr:title"
                       value="${address.title}"/>
                <p class="ui-input-desc" data-for="${pageId}-title">
                    Provide a descriptive title for the address.
                </p>
            </div>
            <div data-role="fieldcontain">
                <label for="${pageId}-hostname">Hostname *</label>
                <input id="${pageId}-hostname" type="text" required="required"
                       name="./<%=AddressBookConstants.PROP_HOSTNAME%>"
                       value="${address.hostname}"/>
                <p class="ui-input-desc" data-for="${pageId}-hostname">
                    Specify the hostname of the remote repository.
                </p>
            </div>
            <div data-role="fieldcontain">
                <label for="${pageId}-port">Port</label>
                <input id="${pageId}-port" type="text" placeholder="default: ${defaultPort}"
                       name="./<%=AddressBookConstants.PROP_PORT%>"
                       value="${address.port}"/>
                <p class="ui-input-desc" data-for="${pageId}-port">
                    Specify the port of the remote repository.
                </p>
            </div>
            <div data-role="fieldcontain">
                <label for="${pageId}-username">Username</label>
                <input id="${pageId}-username" type="text" placeholder="default: ${defaultUsername}"
                       name="./<%=AddressBookConstants.PROP_USERNAME%>"
                       value="${address.username}"/>
                <p class="ui-input-desc" data-for="${pageId}-username">
                    Specify a username with read permission on the content in the remote repository that you wish to copy.
                </p>
            </div>
            <div data-role="fieldcontain">
                <label for="${pageId}-password">Password</label>
                <input id="${pageId}-password" type="password" placeholder="default: ${defaultPassword}"
                       name="./<%=AddressBookConstants.PROP_PASSWORD%>"
                       value="${address.password}"/>
                <p class="ui-input-desc" data-for="${pageId}-password">
                    Specify the password for the account with read permission on the content in the remote repository that you wish to copy.
                </p>
            </div>
            <div data-role="fieldcontain">
                <p class="ui-input-desc">
                    <strong>Be aware that unless you are connecting using HTTPS, your credentials will be sent as
                        cleartext for all sync traffic between the two repositories using this address.
                        If possible, use an account with no more permissions than those provided by membership in the 'contributor' group.</strong>
                </p>
            </div>
            <input id="${pageId}-isHttpsDelete" name="./<%=AddressBookConstants.PROP_IS_HTTPS%>@Delete" value="true" type="hidden"/>
            <input id="${pageId}-isHttpsTypeHint" name="./<%=AddressBookConstants.PROP_IS_HTTPS%>@TypeHint" value="Boolean" type="hidden"/>
            <div data-role="fieldcontain">
                <fieldset data-role="controlgroup">
                    <legend>Protocol</legend>
                    <label for="${pageId}-isHttp">HTTP</label>
                    <input id="${pageId}-isHttp" type="radio" value="false"
                           <% if (address == null || Boolean.FALSE.equals(address.isHttps())) { %>checked="true"<% } %>
                           name="./<%=AddressBookConstants.PROP_IS_HTTPS%>" />
                    <label for="${pageId}-isHttps">HTTPS</label>
                    <input id="${pageId}-isHttps" type="radio" value="true"
                           <% if (address != null && Boolean.TRUE.equals(address.isHttps())) { %>checked="true"<% } %>
                           name="./<%=AddressBookConstants.PROP_IS_HTTPS%>" />
                    <p class="ui-input-desc" data-for="${pageId}-isHttps">
                        Choose HTTPS if the remote server supports SSL encryption
                    </p>
                </fieldset>
            </div>
            <div data-role="fieldcontain">
                <label for="${pageId}-servletPath">DavEx Servlet Path</label>
                <input id="${pageId}-servletPath" type="text" placeholder="default: ${defaultServletPath}"
                       name="./<%=AddressBookConstants.PROP_SERVLET_PATH%>"
                       value="${address.servletPath}"/>
                <p class="ui-input-desc" data-for="${pageId}-servletPath">
                    If the remote repository DAVex servlet is hosted at a different servlet path than the default (for instance, if it is not a CRX repository), make sure to specify it here.                </p>
            </div>
        </form>
    </div>

    <div data-role="footer">
        <div class="g-buttonbar">
            <% if (!ResourceUtil.isStarResource(resource)) { %>
            <a class="delete ui-btn-right" href="#" id="${pageId}-delete" onclick="_g.recap.deleteAddress('${resource.path}')" data-icon="delete" data-iconpos="notext">Delete</a>
            <% } %>
            <a class="done ui-btn-right" href="#" data-icon="save" data-iconpos="notext">Save</a>
        </div>
    </div>

    <script type="text/javascript">
        _g.$('#<%=pageId%> .done').click(function() {
            var qs = "";
            if (_g.recap.getQuickPaths().length > 0) {
                qs = "?<%=AddressBookConstants.RP_PATHS%>=" + _g.recap.getQuickPaths();
            }
            var form = _g.$('#<%=pageId%> form');
            var action = form.attr("action");
            var data = form.serialize();
            _g.$.ajax({url: action, data: data, type: "POST"}).done(function(respData, status, xhr){
                if (xhr.status == 201) {
                    _g.$.mobile.changePage(xhr.getResponseHeader("location")+".html" + qs, {pageContainer:_g.$('#g-recap-main')});
                } else {
                    _g.$.mobile.changePage("${slingRequest.contextPath}${resource.path}.html" + qs, {pageContainer:_g.$('#g-recap-main')});
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
<%@ page import="com.adobe.granite.security.user.UserPropertiesManager" %>
<%@ page import="org.apache.jackrabbit.api.security.user.User" %>
<%@ page import="com.adobe.granite.security.user.UserProperties" %>
<%@ page import="org.apache.sling.api.resource.Resource" %>
<%--
  Recap Remote Sources component.
--%><%
%><%@page session="false" %><%
%><%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.0" %><%
%><sling:defineObjects /><%
%><%
    Resource sources;
    User user = resourceResolver.adaptTo(User.class);
    if (user == null) {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    } else {
        Resource userResource = resourceResolver.getResource(user.getPath());
        if (userResource == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        } else {
            Resource recapSources = userResource.getChild("recapSources");
            if (recapSources != null) {
                sources = recapSources;
            } else {
                userResource.adaptTo(Node.class)
            }
        }
    }
%>
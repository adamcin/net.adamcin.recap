<%@ page import="net.adamcin.recap.api.Recap" %>
<%@ page import="org.json.JSONException" %>
<%@ page import="org.json.JSONWriter" %>
<%@ page import="net.adamcin.recap.addressbook.AddressBook" %>
<%--
  Recap Console component.
--%><%
%><%@page session="false" %><%
%><%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.0" %><%
%><sling:defineObjects /><%
%><%

    Recap recap = sling.getService(Recap.class);
    if (recap == null) {
        slingResponse.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    } else {
        slingResponse.setContentType("application/json");
        slingResponse.setCharacterEncoding("utf-8");

        JSONWriter writer = new JSONWriter(out);

        try {
            writer.object();

            AddressBook addressBook = resourceResolver.adaptTo(AddressBook.class);
            if (addressBook != null) {
                writer.key("addressBookPath").value(addressBook.getResource().getPath());
            }

            writer.key("defaults");
            writer.object();

            writer.key("port").value(recap.getDefaultPort());
            writer.key("username").value(recap.getDefaultUsername());
            writer.key("password").value(recap.getDefaultPassword());
            writer.key("contextPath").value(recap.getDefaultContextPath());
            writer.key("batchSize").value(recap.getDefaultBatchSize());
            writer.key("lastModifiedProperty").value(recap.getDefaultLastModifiedProperty());
            writer.endObject();


            writer.endObject();
        } catch (JSONException e) {
            throw new ServletException(e);
        }
    }

%>
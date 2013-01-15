<%@ page import="net.adamcin.recap.api.Recap" %>
<%@ page import="org.json.JSONException" %>
<%@ page import="org.json.JSONWriter" %>
<%@ page import="net.adamcin.recap.addressbook.AddressBook" %>
<%@ page import="net.adamcin.recap.addressbook.Address" %>
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
                for (Address address : addressBook.listAddresses()) {
                    String path = address.getResource().getPath();
                    writer.key(path).object();

                    writer.key("path").value(path);
                    writer.key("title").value(address.getTitle());
                    writer.key("url").value(recap.getDisplayableUrl(address));

                    writer.endObject();
                }
            }

            writer.endObject();
        } catch (JSONException e) {
            throw new ServletException(e);
        }
    }

%>
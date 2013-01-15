<%@ page import="net.adamcin.recap.api.Recap" %>
<%@ page import="org.json.JSONException" %>
<%@ page import="org.json.JSONWriter" %>
<%@ page import="net.adamcin.recap.addressbook.AddressBook" %>
<%@ page import="net.adamcin.recap.addressbook.Address" %>
<%@ page import="org.apache.sling.api.resource.ValueMap" %>
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
                    writer.key(address.getResource().getPath()).object();

                    ValueMap props = address.getResource().adaptTo(ValueMap.class);
                    if (props != null) {
                        for (ValueMap.Entry<String, Object> entry : props.entrySet()) {
                            writer.key(entry.getKey()).value(entry.getValue());
                        }
                    }

                    writer.endObject();
                }
            }

            writer.endObject();
        } catch (JSONException e) {
            throw new ServletException(e);
        }
    }

%>
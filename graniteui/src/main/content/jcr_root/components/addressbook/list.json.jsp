<%@ page import="net.adamcin.recap.addressbook.Address" %>
<%@ page import="net.adamcin.recap.addressbook.AddressBook" %>
<%@ page import="net.adamcin.recap.api.Recap" %>
<%@ page import="org.json.JSONException" %>
<%@ page import="org.json.JSONWriter" %>
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
%><sling:defineObjects /><%!
	public static String getConnectionKey(Address address) {
		if (address != null) {
			return String.format("%s://%s:%d", address.isHttps() ? "https"
					: "http", address.getHostname(), address.getPort());
		} 
		return null;
	} 
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
					writer.key("connectionKey").value(getConnectionKey(address));
                    writer.endObject();
                }
            }

            writer.endObject();
        } catch (JSONException e) {
            throw new ServletException(e);
        }
    }

%>
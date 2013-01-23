<%@ page import="net.adamcin.recap.addressbook.Address" %>
<%@ page import="net.adamcin.recap.addressbook.AddressBookConstants" %>
<%@ page import="net.adamcin.recap.api.Recap" %>
<%@ page import="net.adamcin.recap.api.RecapException" %>
<%@ page import="net.adamcin.recap.api.RecapOptions" %>
<%@ page import="net.adamcin.recap.api.RecapSession" %>
<%@ page import="net.adamcin.recap.util.HtmlProgressListener" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ page import="javax.jcr.Session" %>
<%@ page import="java.io.PrintWriter" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>
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
    response.setContentType("text/html");
    if (recap == null) {
        response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        return;
    }

    Address address = resource.adaptTo(Address.class);
    if (address == null) {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
        return;
    }

%><!doctype html>
<html>
<head>
    <meta charset="utf-8">
    <title>Recap | rsync for Adobe CRX!</title>
    <meta name="viewport" content="width=device-width, minimum-scale=1, maximum-scale=1">
</head>
<body>

<div style="font-family: monospace">
    <%
        try {
            RecapOptions recapOptions = slingRequest.adaptTo(RecapOptions.class);
            RecapSession recapSession = recap.initSession(slingRequest.getResourceResolver().adaptTo(Session.class), address, recapOptions);
            recapSession.setProgressListener(new HtmlProgressListener(new PrintWriter(out)));
            try {
                List<String> paths = new ArrayList<String>();
                String[] rpPaths = request.getParameterValues(AddressBookConstants.RP_PATHS);

                if (rpPaths != null) {
                    for (String path : rpPaths) {
                        if (path.indexOf('\n') >= 0) {
                            String[] _paths = StringUtils.split(path, '\n');
                            for (String _path : _paths) {
                                paths.add(_path.trim());
                            }
                        } else {
                            paths.add(path.trim());
                        }
                    }
                }

                for (String path : paths) {
                    if (StringUtils.isNotEmpty(path) && path.startsWith("/")) {
                        recapSession.remoteCopy(path);
                    }
                }
            } finally {
                recapSession.finish();
            }
        } catch (RecapException e) {
            throw new ServletException(e);
        }

    %>
    <script type="text/javascript">
        window.scrollTo(0, 1000000);
    </script>
</div>

</body>
</html>
<%@ page import="com.day.cq.widget.HtmlLibraryManager" %>
<%@ page import="net.adamcin.recap.addressbook.Address" %>
<%@ page import="net.adamcin.recap.addressbook.AddressBookConstants" %>
<%@ page import="net.adamcin.recap.api.Recap" %>
<%@ page import="org.apache.commons.lang.StringEscapeUtils" %>
<%@ page import="org.apache.sling.api.resource.ResourceUtil" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="net.adamcin.recap.api.RecapOptions" %>
<%@ page import="net.adamcin.recap.api.RecapSession" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>
<%@ page import="net.adamcin.recap.api.RecapProgressListener" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ page import="net.adamcin.recap.api.RecapException" %>
<%@ page import="net.adamcin.recap.util.HtmlProgressListener" %>
<%@ page import="java.io.PrintWriter" %>
<%@ page import="javax.jcr.Session" %>
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

    String title = "Perform Copy";

    /*
    final PrintWriter printWriter = new PrintWriter(out);
    RecapProgressListener listener = new RecapProgressListener() {
        public void onMessage(String fmt, Object... args) {
            printWriter
        }

        public void onError(String path, Exception ex) {
        }

        public void onFailure(String path, Exception ex) {
        }

        public void onPath(PathAction action, int count, String path) {
        }
    };
    */
%><!doctype html>
<html>
<head>
    <meta charset="utf-8">
    <title>Recap for Adobe CRX | <%=title%></title>
    <meta name="viewport" content="width=device-width, minimum-scale=1, maximum-scale=1">
    <%--
        HtmlLibraryManager htmlMgr = sling.getService(HtmlLibraryManager.class);
        if (htmlMgr != null) {
            htmlMgr.writeIncludes(slingRequest, out, "recap");
        }
    --%>
</head>
<body>
<%--<div data-role="page" id="g-recap-address-console" data-url="<%=request.getRequestURI()%>">--%>

        <div data-role="content" style="font-family: monospace" data-enhance="false">
            <script>

            </script>
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
            <script>
                window.scrollTo(0, 1000000);
            </script>
        </div>

    <%--</div>--%>

</body>
</html>
<%@ page import="net.adamcin.recap.api.RecapRemoteException" %>
<%@ page import="org.json.JSONException" %>
<%@ page import="net.adamcin.recap.api.RecapAddress" %>
<%@ page import="java.util.List" %>
<%@ page import="net.adamcin.recap.api.RecapStrategyDescriptor" %>
<%@ page import="net.adamcin.recap.api.Recap" %>
<%@ page import="net.adamcin.recap.api.RecapConstants" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ page import="org.json.JSONWriter" %>
<%--

  Recap Remote Strategies component.

  

--%><%
%><%@page session="false" %><%
%><%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.0" %><%
%><sling:defineObjects /><%
%><%
    response.setContentType("application/json");
    response.setCharacterEncoding("utf-8");

    Recap recap = sling.getService(Recap.class);

    if (recap == null) {
        log.error("failed to retrieve Recap service from Sling");
        response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    } else {
        try {

            RecapAddress sourceContext = slingRequest.adaptTo(RecapAddress.class);
            List<RecapStrategyDescriptor> strategies;

            JSONWriter jsonWriter = new JSONWriter(response.getWriter());
            jsonWriter.array();

            if (sourceContext != null) {
                strategies = recap.listRemoteStrategies(sourceContext);

                for (RecapStrategyDescriptor strategy : strategies) {
                    if (StringUtils.isNotEmpty(strategy.getType())) {
                        jsonWriter.object();

                        jsonWriter.key(RecapConstants.KEY_STRATEGY_TYPE).value(strategy.getType());

                        if (StringUtils.isNotEmpty(strategy.getLabel())) {
                            jsonWriter.key(RecapConstants.KEY_STRATEGY_LABEL).value(strategy.getLabel());
                        }

                        if (StringUtils.isNotEmpty(strategy.getDescription())) {
                            jsonWriter.key(RecapConstants.KEY_STRATEGY_DESCRIPTION).value(strategy.getDescription());
                        }

                        jsonWriter.endObject();
                    }
                }
            }

            jsonWriter.endArray();
        } catch (RecapRemoteException e) {
            throw new ServletException(e);
        } catch (JSONException e) {
            throw new ServletException(e);
        }
    }
%>
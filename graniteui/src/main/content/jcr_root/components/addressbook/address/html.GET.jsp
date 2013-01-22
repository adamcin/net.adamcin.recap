<%@ page import="net.adamcin.recap.addressbook.Address" %>
<%@ page import="net.adamcin.recap.addressbook.AddressBookConstants" %>
<%@ page import="net.adamcin.recap.api.Recap" %>
<%@ page import="net.adamcin.recap.api.RecapConstants" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>
<%--
  Recap Console component.
--%><%
%><%@page session="false" %><%
%><%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.0" %><%
%><sling:defineObjects /><%
%><%
    Recap recap = sling.getService(Recap.class);
    if (recap != null) {
        pageContext.setAttribute("defaultLastModifiedProperty", recap.getDefaultLastModifiedProperty());
        pageContext.setAttribute("defaultBatchSize", recap.getDefaultBatchSize());
    }
    Address address = resource.adaptTo(Address.class);
    String title = "Sync Content with " + (StringUtils.isNotBlank(address.getTitle()) ? address.getTitle() : address.toString());
    pageContext.setAttribute("title", title);
    String pageId = "g-recap-address-sync-" + resource.getName();
    pageContext.setAttribute("pageId", pageId);

%><!doctype html>
<html>
<head>
    <meta charset="utf-8">
    <title>Recap for Adobe CRX | ${title}</title>
</head>
<body>
    <div data-role="page" id="${pageId}" data-url="${slingRequest.requestURI}" data-title="${title}">

        <div data-role="header">
            <h1 class="g-uppercase">${title}</h1>
        </div>

        <div data-role="content">
            <form action="${slingRequest.contextPath}${resource.path}.graniteconsole.html" target="console_frame" method="post">
                <div data-role="fieldcontain">
                    <label for="${pageId}-paths">Paths *</label>
                    <textarea id="${pageId}-paths" type="text" required="required"
                           name="<%=AddressBookConstants.RP_PATHS%>" cols="40" rows="8"></textarea>
                    <p class="ui-input-desc" data-for="${pageId}-paths">Specify root paths to sync, one per line.</p>
                </div>
                <div data-role="fieldcontain">
                    <fieldset data-role="controlgroup">
                        <legend>Sync Options</legend>
                        <label for="${pageId}-update">Update Existing Nodes</label>
                        <input id="${pageId}-update" type="checkbox" value="true" checked="checked"
                                name="<%=RecapConstants.RP_UPDATE %>"/>
                        <p class="ui-input-desc" data-for="${pageId}-update">If checked, properties on existing nodes will be updated from the source repository.</p>
                        <label for="${pageId}-onlyNewer">Only Newer</label>
                        <input id="${pageId}-onlyNewer" type="checkbox" value="true" checked="checked"
                                name="<%=RecapConstants.RP_ONLY_NEWER %>"/>
                        <p class="ui-input-desc" data-for="${pageId}-onlyNewer">If checked, existing nodes will only be updated if the source node is marked as newer than the target node.</p>
                    </fieldset>
                </div>
                <div data-role="fieldcontain">
                    <label class="ui-input-text"></label>
                    <h2>Advanced Options</h2>
                </div>
                <div data-role="fieldcontain">
                    <label for="${pageId}-lastModifiedProperty">Last Modified Property</label>
                    <input id="${pageId}-lastModifiedProperty" type="text" placeholder="default: ${defaultLastModifiedProperty}"
                              name="<%=RecapConstants.RP_LAST_MODIFIED_PROPERTY %>"/>
                    <p class="ui-input-desc" data-for="${pageId}-lastModifiedProperty">Specify a Date property (in addition to jcr:lastModified) to compare on source and target nodes to determine which is newer.</p>
                </div>
                <div data-role="fieldcontain">
                    <label for="${pageId}-batchSize">Batch Size</label>
                    <input id="${pageId}-batchSize" type="text" placeholder="default: ${defaultBatchSize}"
                           name="<%=RecapConstants.RP_BATCH_SIZE %>"/>
                    <p class="ui-input-desc" data-for="${pageId}-batchSize">Specify the number of nodes to sync between intermediate session saves.</p>
                </div>
                <div data-role="fieldcontain">
                    <label for="${pageId}-throttle">Throttle</label>
                    <input id="${pageId}-throttle" type="text" placeholder="default: 0"
                           name="<%=RecapConstants.RP_THROTTLE %>"/>
                    <p class="ui-input-desc" data-for="${pageId}-throttle">Specify a number of seconds to wait between nodes.</p>
                </div>
            </form>
        </div>

        <div data-role="footer">
            <div class="g-buttonbar">
                <a class="done ui-btn-right" href="#" data-icon="sync" data-iconpos="notext">Start Sync</a>
            </div>
        </div>

        <script type="text/javascript">
            _g.$('#${pageId} .done').click(function() {
                _g.recap.executeSyncToConsole(_g.$('#${pageId} form'));
            });
            _g.$('#${pageId}-update').change(function(e) {
                if (this.checked) {
                    _g.$('#${pageId}-onlyNewer').checkboxradio('enable');
                    _g.$('#${pageId}-lastModifiedProperty').textinput('enable');
                } else {
                    _g.$('#${pageId}-onlyNewer').checkboxradio('disable');
                    _g.$('#${pageId}-lastModifiedProperty').textinput('disable');
                }
            });
        </script>
    </div>

</body>
</html>
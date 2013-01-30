<%@ page import="net.adamcin.recap.addressbook.Address" %>
<%@ page import="net.adamcin.recap.addressbook.AddressBookConstants" %>
<%@ page import="net.adamcin.recap.api.Recap" %>
<%@ page import="net.adamcin.recap.api.RecapConstants" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>
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
        pageContext.setAttribute("defaultLastModifiedProperty", recap.getDefaultLastModifiedProperty());
        pageContext.setAttribute("defaultBatchSize", recap.getDefaultBatchSize());
        pageContext.setAttribute("defaultBatchReadConfig", recap.getDefaultBatchReadConfig());
    }
    Address address = resource.adaptTo(Address.class);
    pageContext.setAttribute("address", address);
    String title = "Sync Content with " + (StringUtils.isNotBlank(address.getTitle()) ? address.getTitle() : address.toString());
    pageContext.setAttribute("title", title);
    String pageId = "g-recap-address-sync-" + resource.getName();
    pageContext.setAttribute("pageId", pageId);

%><!doctype html>
<html>
<head>
    <meta charset="utf-8">
    <title>Recap | rsync for Adobe CRX!</title>
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
                        <p class="ui-input-desc" data-for="${pageId}-update">
                            If checked, properties on existing nodes will be updated from the source repository.
                        </p>
                        <label for="${pageId}-onlyNewer">Only Newer</label>
                        <input id="${pageId}-onlyNewer" type="checkbox" value="true" checked="checked"
                                name="<%=RecapConstants.RP_ONLY_NEWER %>"/>
                        <p class="ui-input-desc" data-for="${pageId}-onlyNewer">
                            If checked, existing nodes will only be updated if the source node is marked as newer than the target node.
                        </p>
                        <label for="${pageId}-noRecurse">Non-Recursive</label>
                        <input id="${pageId}-noRecurse" type="checkbox" value="true"
                               name="<%=RecapConstants.RP_NO_RECURSE %>"/>
                        <p class="ui-input-desc" data-for="${pageId}-noRecurse">
                            If checked, only the properties on each path specified above will be synced. Their descendants will be ignored.
                        </p>
                        <label for="${pageId}-reverse">Reverse</label>
                        <input id="${pageId}-reverse" type="checkbox" value="true"
                               name="<%=RecapConstants.RP_REVERSE %>"/>
                        <p class="ui-input-desc" data-for="${pageId}-reverse">
                            If checked, the local repository will serve as the source for the specified paths, and the
                            <strong>content will be copied to ${address}</strong>. Obviously, this assumes that the credentials
                            specified in the address provide WRITE permissions for the specified paths and their descendants
                            in the remote repository.
                        </p>
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
                    <p class="ui-input-desc" data-for="${pageId}-batchSize">
                        Specify the number of nodes to sync between intermediate session saves.</p>
                </div>
                <div data-role="fieldcontain">
                    <label for="${pageId}-throttle">Throttle</label>
                    <input id="${pageId}-throttle" type="text" placeholder="default: 0"
                           name="<%=RecapConstants.RP_THROTTLE %>"/>
                    <p class="ui-input-desc" data-for="${pageId}-throttle">
                        Specify a number of seconds to wait between nodes. </p>
                </div>
                <div data-role="fieldcontain">
                    <label for="${pageId}-batchReadConfig">Batch Read Config (Very advanced)</label>
                    <textarea id="${pageId}-batchReadConfig" type="text"
                              name="<%=RecapConstants.RP_BATCH_READ_CONFIG %>" cols="40" rows="8" placeholder="default: ${defaultBatchReadConfig}"></textarea>
                    <p class="ui-input-desc" data-for="${pageId}-batchReadConfig">
                        Specify a whitespace delimited list of JCR DavEx batch read config entries.
                        An entry may be either an integer depth or a token of the format
                        "[path]=[depth]", where <strong>[path]</strong> is a repository path and
                        <strong>[depth]</strong> is an integer representing the depth to read in batch
                        for the specified path. All depth-only entries will be collected, in order,
                        to represent the batch-read-depth for paths whose repository depth matches
                        the 0-based index of the entry. The last depth-only entry listed will be used as
                        the default batch read depth for all paths not specified by other entries.
                        For example, if the value of this field is "2 1 3", DavEx will download two
                        levels of descendants when the root path ("/") is requested, one level of
                        descendants if "/content" is requested, and three levels of descendants if
                        "/content/geometrixx" or any of its children or descendants are requested.
                    </p>
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
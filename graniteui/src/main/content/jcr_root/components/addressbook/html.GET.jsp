<%@ page import="com.day.cq.widget.HtmlLibraryManager" %>
<%@ page import="net.adamcin.recap.addressbook.Address" %>
<%@ page import="net.adamcin.recap.addressbook.AddressBookConstants" %>
<%@ page import="net.adamcin.recap.api.Recap" %>
<%@ page import="org.apache.commons.lang.StringEscapeUtils" %>
<%@ page import="org.apache.sling.api.resource.ResourceUtil" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="net.adamcin.recap.addressbook.AddressBook" %>
<%--
  Recap Console component.
--%><%
%><%@page session="false" %><%
%><%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.0" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
%><sling:defineObjects /><%
%><%
    Recap recap = sling.getService(Recap.class);
    if (recap != null) {
        AddressBook addressBook = resourceResolver.adaptTo(AddressBook.class);
        if (addressBook != null) {
            pageContext.setAttribute("addresses", addressBook.listAddresses().iterator());
        }
    }
%><!doctype html>
<html>
<head>
    <meta charset="utf-8">
    <title>Recap for Adobe CRX | Address Book</title>
    <meta name="viewport" content="width=device-width, minimum-scale=1, maximum-scale=1">
    <%
        HtmlLibraryManager htmlMgr = sling.getService(HtmlLibraryManager.class);
        if (htmlMgr != null) {
            htmlMgr.writeIncludes(slingRequest, out, "recap");
        }
    %>
</head>
<body>
<div id="g-recap-address-book" data-role="page" data-url="<%=request.getRequestURI()%>">
    <div data-role="header">
        <h1 class="g-uppercase">Address Book</h1>
    </div>

    <div data-role="content" data-scroll="y" data-theme="c">
        <ul id="address-list" data-role="listview" data-split-theme="c" data-split-icon="gear">
            <c:forEach var="address" items="${addresses}">
                <li>
                    <a href="${request.contextPath}${address.resource.path}.html" data-panel="main">
                        <h3>${address.title}</h3>
                        <p>${address}</p>

                        <%--
                        <div data-role="controlgroup" data-type="horizontal">
                            <a href="${request.contextPath}${address.resource.path}.delete.html" data-icon="delete" data-role="button" data-iconpos="notext" data-panel="main">Delete</a>
                        </div>
                        --%>
                    </a>
                    <a href="${request.contextPath}${address.resource.path}.edit.html" data-panel="main">Edit</a>
                </li>
            </c:forEach>
        </ul>
    </div>

    <div data-role="footer">
        <div class="g-buttonbar" id="addressbook-buttonbar">
            <a id="g-recap-address-book-new-address" data-icon="plus"
               href="${request.contextPath}${resource.path}/*.edit.html" data-role="button" data-panel="main">New Address</a>
        </div>
    </div>
</div>
</body>
</html>

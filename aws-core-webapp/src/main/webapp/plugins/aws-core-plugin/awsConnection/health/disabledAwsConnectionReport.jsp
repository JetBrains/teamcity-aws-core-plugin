<%@include file="/include-internal.jsp" %>
<%@ page import="jetbrains.buildServer.web.openapi.healthStatus.HealthStatusItemDisplayMode" %>
<%@ page import="jetbrains.buildServer.clouds.amazon.connector.health.DisabledAwsConnectionHealthReport" %>
<c:set var="inplaceMode" value="<%=HealthStatusItemDisplayMode.IN_PLACE%>"/>

<jsp:useBean id="healthStatusItem" type="jetbrains.buildServer.serverSide.healthStatus.HealthStatusItem" scope="request"/>
<c:set var="disabledAwsConnections" value="${healthStatusItem.additionalData[DisabledAwsConnectionHealthReport.DISABLED_AWS_CONNECTIONS_PARAM]}"/>
<c:set var="numDisabledAwsConnections" value="${disabledAwsConnections.size()}"/>

<div class="suggestionItem">

  AWS Connection<bs:s val="${numDisabledAwsConnections}"/> with the following Connection ID<bs:s val="${numDisabledAwsConnections}"/> <bs:are_is val="${numDisabledAwsConnections}"/> disabled:
  <br>
  <ul>
    <c:forEach var="projectConnIdsPair" items="${disabledAwsConnections}">
      <c:set var="editConnectionUrl"><c:url value='/admin/editProject.html?projectId=${projectConnIdsPair.first}&tab=oauthConnections'/></c:set>
      <li><a href="${editConnectionUrl}" target="_blank" rel="noreferrer"><c:out value="${projectConnIdsPair.second}"/></a></li>
    </c:forEach>
  </ul>

  Remove
  <c:choose>
    <c:when test="${numDisabledAwsConnections == 1}"> this </c:when>
    <c:otherwise> these </c:otherwise>
  </c:choose>
  connection<bs:s val="${numDisabledAwsConnections}"/> or contact your server administrator.
  <br>
  More information here <bs:helpLink file="configuring-connections#AmazonWebServices"><bs:helpIcon/></bs:helpLink>
</div>
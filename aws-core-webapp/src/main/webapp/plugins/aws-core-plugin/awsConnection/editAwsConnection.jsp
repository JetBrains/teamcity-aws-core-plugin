<%@include file="awsConnectionConstants.jspf" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="intprop" uri="/WEB-INF/functions/intprop" %>

<jsp:useBean id="project" type="jetbrains.buildServer.serverSide.SProject" scope="request"/>

<c:set var="awsConnectionReactUiEnabledGlobal" value="${intprop:getBoolean(react_ui_enabled)}"/>
<c:set var="awsConnectionReactUiEnabled" value="${project.parameters.getOrDefault(react_ui_enabled, awsConnectionReactUiEnabledGlobal)}"/>

<c:choose>
  <c:when test="${awsConnectionReactUiEnabled}">
    <jsp:include page="editAwsConnection_react.jsp" />
  </c:when>
  <c:otherwise>
    <jsp:include page="editAwsConnection_old.jsp" />
  </c:otherwise>
</c:choose>

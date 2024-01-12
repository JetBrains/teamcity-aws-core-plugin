

<%@ include file="/include-internal.jsp"%>
<%@ taglib prefix="intprop" uri="/WEB-INF/functions/intprop" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>

<%@include file="sessionCredentialsConst.jspf"%>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>
<c:set var="sessionCredsDuration" value="${empty param.sessionDuration ? propertiesBean.properties[session_duration_param] : param.sessionDuration}"/>

<tr id="${session_duration_param}_row">
  <th><label for="${session_duration_param}">${session_duration_label}</label></th>
  <td><props:textProperty name="${session_duration_param}"
                          value="${empty sessionCredsDuration ? session_duration_default : sessionCredsDuration}" className="longField" maxlength="256"/>
    <span class="smallNote">This field is only for temporary credentials. The value is specified in minutes: from 15 to 2160 (36 h).</span>
    <span class="error" id="error_${session_duration_param}"></span>
  </td>
</tr>

<%@ include file="/include-internal.jsp"%>
<%@ taglib prefix="intprop" uri="/WEB-INF/functions/intprop" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>

<%@include file="../../awsConnectionConstants.jspf"%>
<%@include file="defaultCredsProviderConstants.jspf"%>

<tr>
  <th></th>
  <td>
    <c:if test="${intprop:getBoolean(default_creds_provider_prop_name) == 'false'}">
      <span class="error" style="white-space:pre-wrap; word-break:break-word;">The <b>Default Credentials Provider Chain</b> type is disabled on this server. For instructions on how to enable it and for more information see the <bs:helpLink file="configuring-connections#AmazonWebServices">documentation</bs:helpLink></span>
    </c:if>
  </td>
</tr>
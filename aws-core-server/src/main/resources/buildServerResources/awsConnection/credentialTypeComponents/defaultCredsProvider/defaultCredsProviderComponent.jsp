<%--
  ~ Copyright 2000-2022 JetBrains s.r.o.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~ http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  --%>
<%@ include file="/include-internal.jsp"%>
<%@ taglib prefix="intprop" uri="/WEB-INF/functions/intprop" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>

<%@include file="../../awsConnectionConstants.jspf"%>
<%@include file="defaultCredsProviderConstants.jspf"%>

<tr>
  <th></th>
  <td>
    <c:choose>
      <c:when test="${intprop:getProperty(default_creds_provider_prop_name, 'false') == 'false'}">
        <span class="error" style="white-space:pre-wrap; word-break:break-word;">The <b>Default Credentials Provider Chain</b> type is disabled on this server. For instructions on how to enable it and for more information see the <bs:helpLink file="configuring-connections#AmazonWebServices">documentation</bs:helpLink></span>
      </c:when>

      <c:otherwise/>
    </c:choose>
  </td>
</tr>
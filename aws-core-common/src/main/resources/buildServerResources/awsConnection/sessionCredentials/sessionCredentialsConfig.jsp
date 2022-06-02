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
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>

<%@include file="sessionCredentialsConst.jspf"%>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>
<c:set var="sessionCredsDuration" value="${propertiesBean.properties[session_duration_param]}"/>

<tr id="${session_duration_param}_row">
  <th><label for="${session_duration_param}">${session_duration_label}</label></th>
  <td><props:textProperty name="${session_duration_param}"
                          value="${empty sessionCredsDuration ? session_duration_default : sessionCredsDuration}" className="longField" maxlength="256"/>
    <span class="smallNote">In minutes. From 15 to 2160 (36 h). </span>
    <span class="error" id="error_${session_duration_param}"></span>
  </td>
</tr>
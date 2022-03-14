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
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>

<%@include file="awsAccessKeysCredsConstants.jspf" %>

<c:choose>
    <c:when test = "${param.connectionId != ''}">
        <c:set var="useSessionCreds" value="${param.useSessionCreds}"/>
        <c:set var="sessionCredsDuration" value="${param.sessionCredsDuration}"/>
        <c:set var="stsEndpoint" value="${param.stsEndpoint}"/>
    </c:when>

    <c:otherwise>
        <c:set var="useSessionCreds" value="${use_session_credentials_default}"/>
        <c:set var="sessionCredsDuration" value="${session_duration_default}"/>
        <c:set var="stsEndpoint" value="${sts_endpoint_default}"/>
    </c:otherwise>
</c:choose>

<l:settingsGroup title="Access Key">
    <tr id="${access_key_id_param}_row">
        <th><label for="${access_key_id_param}">${access_key_id_label}: <l:star/></label></th>
        <td><props:textProperty name="${access_key_id_param}" className="longField" maxlength="256" noAutoComplete="true"/>
            <span class="error" id="error_${access_key_id_param}"></span>
        </td>
    </tr>
    <tr id="${secret_access_key_param}_row">
        <th class="nowrap"><label for="${secure_secret_access_key_param}">${secret_access_key_label}: <l:star/></label></th>
        <td><props:passwordProperty name="${secure_secret_access_key_param}" className="longField" maxlength="256"/>
            <span class="error" id="error_${secure_secret_access_key_param}"></span>
        </td>
    </tr>
</l:settingsGroup>

<l:settingsGroup title="Session Settings">
    <tr>
        <th><label for="${use_session_credentials_param}">${use_session_credentials_label}</label></th>
        <td>
            <props:checkboxProperty name="${use_session_credentials_param}" checked="${useSessionCreds}"/>
            <span>Issue temporary credentials by request</span>
        </td>
    </tr>

    <tr id="${session_duration_param}_row">
        <th><label for="${session_duration_param}">${session_duration_label}</label></th>
        <td><props:textProperty name="${session_duration_param}" value="${sessionCredsDuration}" className="longField" maxlength="256"/>
            <span class="smallNote">In seconds. From 900 (15 min) to 129600 (36 h). </span>
            <span class="error" id="error_${session_duration_param}"></span>
        </td>
    </tr>

    <tr id="${sts_endpoint_param}_row">
        <th><label for="${sts_endpoint_param}">${sts_endpoint_label}</label></th>
        <td><props:textProperty name="${sts_endpoint_param}" value="${stsEndpoint}" className="longField" maxlength="256"/>
            <span class="smallNote">Default is: ${sts_endpoint_default}</span>
            <span class="error" id="error_${sts_endpoint_param}"></span>
        </td>
    </tr>
</l:settingsGroup>
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

<%@include file="awsIamRoleCredsConstants.jspf" %>
<%@include file="../../awsConnectionConstants.jspf"%>

<jsp:useBean id="project" type="jetbrains.buildServer.serverSide.SProject" scope="request"/>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>
<jsp:useBean id="oauthConnectionBean" scope="request" type="jetbrains.buildServer.serverSide.oauth.OAuthConnectionBean"/>

<c:set var="stsEndpoint" value="${propertiesBean.properties[sts_endpoint_param]}"/>
<c:set var="iamRoleArn" value="${propertiesBean.properties[sts_endpoint_param]}"/>
<c:set var="sessionName" value="${propertiesBean.properties[iam_role_session_name_param]}"/>

<props:hiddenProperty name="${project_id_param}" value="${project.externalId}"/>
<l:settingsGroup title="IAM Role">
    <jsp:include page="${avail_aws_conns_url}">
        <jsp:param name="projectId" value="${project.externalId}"/>
        <jsp:param name="principalAwsConnId" value="${oauthConnectionBean.getConnectionId()}"/>
    </jsp:include>

    <tr id="${iam_role_arn_param}_row">
        <th class="nowrap"><label for="${iam_role_arn_param}">${iam_role_arn_label}: <l:star/></label></th>
        <td><props:textProperty name="${iam_role_arn_param}" className="longField" maxlength="256"/>
            <span class="error" id="error_${iam_role_arn_param}"></span>
        </td>
    </tr>
</l:settingsGroup>

<l:settingsGroup title="Session Settings">
    <tr id="${iam_role_session_name_param}_row">
        <th><label for="${iam_role_session_name_param}">${iam_role_session_name_label}</label></th>
        <td><props:textProperty name="${iam_role_session_name_param}"
                                value="${empty sessionName ? iam_role_session_name_default : sessionName}" className="longField" maxlength="256"/>
            <span class="error" id="error_${iam_role_session_name_param}"></span>
        </td>
    </tr>

    <tr id="${sts_endpoint_param_iam_role}_row">
        <th><label for="${sts_endpoint_param_iam_role}">${sts_endpoint_label}</label></th>
        <td><props:textProperty id="${sts_endpoint_field_id_iam_role}"
                                name="${sts_endpoint_param_iam_role}"
                                value="${stsEndpoint}" className="longField" maxlength="256"/>
            <span class="smallNote">The global endpoint is: ${sts_global_endpoint}</span>
            <span class="error" id="error_${sts_endpoint_param_iam_role}"></span>
        </td>
    </tr>
</l:settingsGroup>

<jsp:include page="../stsEndpointLogic.jsp"/>
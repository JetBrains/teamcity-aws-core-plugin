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
<%@include file="../../sessionCredentials/sessionCredentialsConst.jspf"%>

<jsp:useBean id="project" type="jetbrains.buildServer.serverSide.SProject" scope="request"/>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>
<jsp:useBean id="oauthConnectionBean" scope="request" type="jetbrains.buildServer.serverSide.oauth.OAuthConnectionBean"/>

<c:set var="stsEndpoint" value="${propertiesBean.properties[sts_endpoint_param]}"/>
<c:set var="credsType" value="${propertiesBean.properties[credentials_type_param]}"/>
<c:set var="iamRoleArn" value="${propertiesBean.properties[iam_role_arn_param]}"/>
<c:set var="sessionName" value="${propertiesBean.properties[iam_role_session_name_param]}"/>

<c:url var="externalIdsControllerUrl" value="${external_ids_controller_url}"/>

<l:settingsGroup title="IAM Role">
    <jsp:include page="${avail_aws_conns_url}">
        <jsp:param name="projectId" value="${project.externalId}"/>
        <jsp:param name="principalAwsConnId" value="${oauthConnectionBean.getConnectionId()}"/>
        <jsp:param name="sessionDuration" value="${session_duration_default}"/>
    </jsp:include>

    <tr id="${iam_role_arn_param}_row">
        <th class="nowrap"><label for="${iam_role_arn_param}">${iam_role_arn_label} <l:star/></label></th>
        <td><props:textProperty name="${iam_role_arn_param}" className="longField" maxlength="256"/>
            <span class="smallNote">Pre-configured IAM role with necessary permissions</span>
            <span class="error" id="error_${iam_role_arn_param}" style="word-break: break-all;"></span>
        </td>
    </tr>

    <c:choose>
        <c:when test = "${param.connectionId != '' and credsType == credentials_type_iam_role_option}">
            <tr>
                <th class="nowrap"><label for="${external_id_field_id}">External ID:</label></th>
                <td>
                    <div id="div_${external_id_field_id}">
                        <bs:copy2ClipboardLink dataId="${external_id_field_id}" title="Copy" stripTags="true">
                            <label id="${external_id_field_id}" className="longField" maxlength="256"/>
                        </bs:copy2ClipboardLink>
                    </div>
                    <span class="smallNote">External ID is strongly recommended to be used in role trust relationship condition</span>
                    <span class="error" id="error_${external_id_field_id}" style="word-break: break-all;"></span>
                </td>
            </tr>
        </c:when>

        <c:otherwise/>
    </c:choose>
</l:settingsGroup>

<l:settingsGroup title="Session settings">
    <tr id="${iam_role_session_name_param}_row">
        <th><label for="${iam_role_session_name_param}">${iam_role_session_name_label}</label></th>
        <td><props:textProperty name="${iam_role_session_name_param}"
                                value="${empty sessionName ? iam_role_session_name_default : sessionName}" className="longField" maxlength="256"/>
            <span class="smallNote">Identifies which TeamCity connection assumes the role</span>
            <span class="error" id="error_${iam_role_session_name_param}" style="word-break: break-all;"></span>
        </td>
    </tr>

    <tr id="${sts_endpoint_param}_row">
        <th><label for="${sts_endpoint_field_id_iam_role}">${sts_endpoint_label}</label></th>
        <td><props:textProperty id="${sts_endpoint_field_id_iam_role}"
                                name="${sts_endpoint_param}"
                                value="${stsEndpoint}" className="longField" maxlength="256"/>
            <span class="smallNote">The global endpoint is ${sts_global_endpoint}</span>
            <span class="error" id="error_${sts_endpoint_param}" style="word-break: break-all;"></span>
        </td>
    </tr>
</l:settingsGroup>

<%@ include file="../stsEndpointLogic.jsp" %>


<script type="text/javascript">
    $j(document).ready(function () {
        if("${not empty param.connectionId}" === "true" && "${credsType == credentials_type_iam_role_option}" === "true"){
            BS.ajaxRequest('${externalIdsControllerUrl}', {
                parameters: '&projectId=${param.projectId}&${aws_conn_id_rest_param}=${param.connectionId}',

                onComplete: function(response) {
                    const json = response.responseJSON;
                    const errors = json.errors;

                    const externalIdElement = document.getElementById("${external_id_field_id}");
                    const externalIdDivElement = document.getElementById("div_${external_id_field_id}");
                    const externalIdErrorElement = document.getElementById("error_${external_id_field_id}");
                    if(errors == null) {
                        externalIdErrorElement.textContent = "";
                        externalIdDivElement.classList.remove('hidden');
                        externalIdElement.textContent = json;
                    } else {
                        const externalIdError = errors[0];
                        if (externalIdError != null) {
                            externalIdDivElement.classList.add('hidden');
                            externalIdErrorElement.textContent = externalIdError.message;
                        }
                    }
                }
            });
        }
    });
</script>
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
<%@include file="../../awsConnectionConstants.jspf" %>

<jsp:useBean id="project" type="jetbrains.buildServer.serverSide.SProject" scope="request"/>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>
<c:set var="useSessionCreds" value="${propertiesBean.properties[use_session_credentials_param]}"/>
<c:set var="stsEndpoint" value="${propertiesBean.properties[sts_endpoint_param]}"/>
<c:set var="credsType" value="${propertiesBean.properties[credentials_type_param]}"/>

<c:set var="rotateKeyControllerUrl"><c:url value="${rotate_key_controller_url}"/></c:set>
<c:set var="keyRotatedInfoId" value="info_${rotate_key_button_id}"/>
<c:set var="rotateKeySpinnerId" value="spinner_${rotate_key_button_id}"/>

<l:settingsGroup title="Access Key">
    <tr id="${access_key_id_param}_row">
        <th><label for="${access_key_id_param}">${access_key_id_label}: <l:star/></label></th>
        <td><props:textProperty name="${access_key_id_param}" className="longField" maxlength="256" noAutoComplete="true"/>
            <span class="error" id="error_${access_key_id_param}" style="word-break: break-all;"></span>
        </td>
    </tr>
    <tr id="${secret_access_key_param}_row">
        <th class="nowrap"><label for="${secure_secret_access_key_param}">${secret_access_key_label}: <l:star/></label></th>
        <td><props:passwordProperty name="${secure_secret_access_key_param}" className="longField" maxlength="256"/>
            <span class="error" id="error_${secure_secret_access_key_param}" style="word-break: break-all;"></span>
        </td>
    </tr>

    <c:choose>
        <c:when test = "${param.connectionId != '' and credsType == credentials_type_access_keys_option}">
            <tr>
                <td>
                    <forms:button id="${rotate_key_button_id}"
                                  onclick="return BS.OAuthConnectionDialog.rotateKey('${param.connectionId}');">Rotate key</forms:button>
                    <forms:saving id="${rotateKeySpinnerId}" className="progressRingInline"/>
                </td>
                <td>
                    <span id="error_${rotate_key_button_id}" class="error error_${rotate_key_button_id}"></span>
                    <div class="hidden successMessage" id="${keyRotatedInfoId}"></div>
                </td>
            </tr>
        </c:when>

        <c:otherwise/>
    </c:choose>

</l:settingsGroup>

<l:settingsGroup title="Session Settings">
    <tr class="non_serializable_form_elements_container" id="${use_session_credentials_param}_row">
        <th><label for="${use_session_credentials_param}">${use_session_credentials_label}</label></th>
        <td>
            <props:checkboxProperty id="useSessionCredentialsCheckbox"
                                    name="${use_session_credentials_param}_checkbox"
                                    checked="${empty useSessionCreds ? use_session_credentials_default : useSessionCreds}"
                                    />
            <span>Issue temporary credentials by request</span>
        </td>
    </tr>
    <props:hiddenProperty id="useSessionCredentials" name="${use_session_credentials_param}" value="${empty useSessionCreds ? use_session_credentials_default : useSessionCreds}"/>

    <tr id="${sts_endpoint_param}_row" class="stsEndpointClass">
        <th><label for="${sts_endpoint_param}">${sts_endpoint_label}</label></th>
        <td><props:textProperty id="${sts_endpoint_field_id}"
                                name="${sts_endpoint_param}"
                                value="${stsEndpoint}" className="longField" maxlength="256"/>
            <span class="smallNote">The global endpoint is: ${sts_global_endpoint}</span>
            <span class="error" id="error_${sts_endpoint_param}" style="word-break: break-all;"></span>
        </td>
    </tr>
</l:settingsGroup>


<script>

    BS.OAuthConnectionDialog._errorIds = [
        '${rotate_key_button_id}'
    ];

    BS.OAuthConnectionDialog.rotateKey = function (connectionId){
        this.toggleKeyRotationState(true);

        BS.ajaxRequest("${rotateKeyControllerUrl}", {
            parameters: 'connectionId=' + connectionId + "&projectId=${project.externalId}",

            onSuccess: function(response) {
                const jsonResponse = response.responseJSON;
                const errors = jsonResponse.errors;

                if (errors.length === 0) {
                    this.close();

                    this.showEditDialog(connectionId, "${aws_connection_dialog_name}", false);

                    //TODO find a better solution to fix the race condition
                    setTimeout(() => {
                        $j("#${keyRotatedInfoId}").append("New key has been saved.");
                        $j("#${keyRotatedInfoId}").removeClass("hidden");
                    }, 200);

                } else {
                    for(let i = 0; i < errors.length; i ++) {
                        this.addError(errors[i].message, errors[i].id)
                    }
                }
            }.bind(this),

            onComplete: function() {
                this.toggleKeyRotationState(false);
            }.bind(this),
        });
    };

    BS.OAuthConnectionDialog.toggleKeyRotationState = function (keyIsRotating){
        if(keyIsRotating){
            $j('#${rotateKeySpinnerId}').show();

            this.clearAllErrors(this._errorIds);
            $j('#${keyRotatedInfoId}').empty();
            $j("#${keyRotatedInfoId}").addClass("hidden");

            $j('#${rotate_key_button_id}').attr('disabled','disabled');
            $j('#testConnectionButton').attr('disabled','disabled');
            $j('.cancel').attr('disabled','disabled');
            this.disable();

        } else {
            $j('#${rotateKeySpinnerId}').hide();

            this.enable();
            $j('#${rotate_key_button_id}').removeAttr('disabled');
            $j('#testConnectionButton').removeAttr('disabled');
            $j('.cancel').removeAttr('disabled');
        }
    };
</script>

<%@ include file="../stsEndpointLogic.jsp" %>
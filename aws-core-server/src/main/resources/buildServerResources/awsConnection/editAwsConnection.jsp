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

<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>
<jsp:useBean id="oauthConnectionBean" scope="request" type="jetbrains.buildServer.serverSide.oauth.OAuthConnectionBean"/>


<%@include file="awsConnectionConstants.jspf" %>
<%@include file="credentialTypeComponents/accessKeys/awsAccessKeysCredsConstants.jspf" %>

<c:url var="testAwsConnectionControllerUrl" value="${test_connection_controller_url}"/>

<c:set var="previouslyChosenRegion" value="${(empty propertiesBean.properties[region_name_param]) ? region_name_default : propertiesBean.properties[region_name_param]}"/>

<bs:linkScript>
  /js/bs/testConnection.js
</bs:linkScript>

<tr>
  <td><label for="displayName">Name:</label><l:star/></td>
  <td>
    <props:textProperty name="displayName" className="longField" style="width: 20em;"/>
    <span class="smallNote nowrap">Provide some name to distinguish this connection from others.</span>
    <span class="error" id="error_displayName"></span>
  </td>
</tr>

<tr>
  <th><label for="${region_name_param}">${region_name_label}: </label></th>
  <td>
    <props:selectProperty id="${region_select_id}" name="${region_name_param}" enableFilter="true">
      <c:forEach var="region" items="${allRegions.keySet()}">
        <props:option value="${region}" selected="${region eq previouslyChosenRegion}"><c:out value="${allRegions[region]}"/></props:option>
      </c:forEach>
    </props:selectProperty>
    <span class="smallNote">The region where this connection will be used</span><span class="error" id="error_${region_name_param}"></span>
  </td>
</tr>

<props:selectSectionProperty name="${credentials_type_param}" title="${credentials_type_label}:">
  <props:selectSectionPropertyContent value="${credentials_type_access_keys_option}" caption="${credentials_type_access_keys_label}">
    <jsp:include page="credentialTypeComponents/accessKeys/awsAccessKeysCredsComponent.jsp">
      <jsp:param name="connectionId" value="${oauthConnectionBean.getConnectionId()}"/>
    </jsp:include>
  </props:selectSectionPropertyContent>
  <props:selectSectionPropertyContent value="${credentials_type_iam_role_option}" caption="${credentials_type_iam_role_label}">
    <jsp:include page="credentialTypeComponents/iamRole/awsIamRoleCredsComponent.jsp"/>
  </props:selectSectionPropertyContent>
</props:selectSectionProperty>

<script>
  BS.OAuthConnectionDialog.submitTestConnection = function () {
    const enableForm = this.enable.bind(this);
    BS.PasswordFormSaver.save(this, '${testAwsConnectionControllerUrl}', OO.extend(BS.ErrorsAwareListener, {
      onFailedTestConnectionError: function (elem) {
        let text = "";
        if (elem.firstChild) {
          text = elem.firstChild.nodeValue;
        }
        BS.TestConnectionDialog.show(false, text, $('testConnectionButton'));
      },
      onCompleteSave: function (form, responseXML) {
        const err = BS.XMLResponse.processErrors(responseXML, this, form.propertiesErrorsHandler);
        BS.ErrorsAwareListener.onCompleteSave(form, responseXML, err);
        if (!err) {
          this.onSuccessfulSave(responseXML);
        }
      },
      onSuccessfulSave: function (responseXML) {
        enableForm();
        const testConnectionResultNodes = responseXML.documentElement.getElementsByTagName('${aws_caller_identity_element}');

        let additionalInfo;
        if (testConnectionResultNodes.length > 0) {
          additionalInfo = "Caller Identity:";
          const testConnectionResult = testConnectionResultNodes.item(0);
          additionalInfo += "\n Account ID: " + testConnectionResult.getAttribute('${aws_caller_identity_attr_account_id}');
          additionalInfo += "\n User ID: " + testConnectionResult.getAttribute('${aws_caller_identity_attr_user_id}');
          additionalInfo += "\n ARN: " + testConnectionResult.getAttribute('${aws_caller_identity_attr_user_arn}');
        } else {
          additionalInfo = "Could not get the Caller Identity information from the response.";
        }

        BS.TestConnectionDialog.show(true, additionalInfo, $('testConnectionButton'));
      }
    }));
    return false;
  };

  const afterClose = BS.OAuthConnectionDialog.afterClose;
  BS.OAuthConnectionDialog.afterClose = function () {
    $j('.testConnectionButton').remove();
    afterClose()
  };

  BS.OAuthConnectionDialog.addError = function (errorHTML, target){
    target.append(errorHTML);
  };
  BS.OAuthConnectionDialog.clearError = function (errorId){
    const target = $j('.error_' + errorId);
    target.empty();
  };
  BS.OAuthConnectionDialog.clearAllErrors = function (errorIdsArray){
    errorIdsArray.forEach(errorId => {
      this.clearError(errorId);
    })
  };
</script>

<forms:button id="testConnectionButton" className="testConnectionButton"
              onclick="return BS.OAuthConnectionDialog.submitTestConnection();">Test Connection</forms:button>

<bs:dialog dialogId="testConnectionDialog" title="Test Connection" closeCommand="BS.TestConnectionDialog.close();"
           closeAttrs="showdiscardchangesmessage='false'">
  <div id="testConnectionStatus"></div>
  <div id="testConnectionDetails" class="mono"></div>
</bs:dialog>

<script>
  $j('#OAuthConnectionDialog .popupSaveButtonsBlock .testConnectionButton').remove();
  $j('#OAuthConnectionDialog .popupSaveButtonsBlock .cancel').after($j('#testConnectionButton'))
</script>


<%@ include file="/include-internal.jsp"%>
<%@ taglib prefix="intprop" uri="/WEB-INF/functions/intprop" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>

<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>
<jsp:useBean id="oauthConnectionBean" scope="request" type="jetbrains.buildServer.serverSide.oauth.OAuthConnectionBean"/>
<jsp:useBean id="project" type="jetbrains.buildServer.serverSide.SProject" scope="request"/>

<%@include file="awsConnectionConstants.jspf" %>
<%@include file="credentialTypeComponents/accessKeys/awsAccessKeysCredsConstants.jspf" %>

<c:url var="testAwsConnectionControllerUrl" value="${test_connection_controller_url}"/>

<c:set var="previouslyChosenRegion" value="${(empty propertiesBean.properties[region_name_param]) ? region_name_default : propertiesBean.properties[region_name_param]}"/>
<c:set var="connectionId" value="${oauthConnectionBean.getConnectionId()}"/>

<bs:linkScript>
  /js/bs/testConnection.js
</bs:linkScript>

<style type="text/css">
  .error {
    word-break: break-all;
  }
</style>

<tr class="awsConnectionNote">
  <td colspan="2">
    Adds a new Connection that allows TeamCity to store and manage AWS Credentials.
    <bs:helpLink file="configuring-connections#AmazonWebServices"><bs:helpIcon/></bs:helpLink>
  </td>
</tr>

<tr>
  <th><label for="${display_name_param}">Display name:</label><l:star/></th>
  <td>
    <props:textProperty name="${display_name_param}" className="longField"/>
    <span class="smallNote nowrap">Provide a name to distinguish this connection from others</span>
    <span class="error" id="error_displayName"></span>
  </td>
</tr>


<th><label for="${connection_id_param}">${connection_id_label}:</label></th>
<td>
  <c:choose>
    <c:when test = "${empty connectionId}">
      <props:textProperty name="${connection_id_param}" className="longField"
                          value="${connectionId}"/>
      <span class="smallNote">This ID is used in URLs, REST API, HTTP requests to the server and configuration settings in the TeamCity Data Directory.</span>
      <script type="application/javascript">
        BS.AdminActions.prepareCustomIdGenerator('${aws_connection_id_generator_type}', ${connection_id_param}, ${display_name_param});
      </script>
    </c:when>
    <c:otherwise>
      <label style="word-break: break-all;">${connectionId}</label>
    </c:otherwise>
  </c:choose>
  <span class="error" id="error_${connection_id_param}"></span>
</td>



<tr>
  <th><label for="${region_name_param}">${region_name_label}: </label></th>
  <td>
    <props:selectProperty id="${region_select_id}" name="${region_name_param}" enableFilter="true" style="width: 20em;">
      <c:forEach var="region" items="${allRegions.keySet()}">
        <props:option value="${region}" selected="${region eq previouslyChosenRegion}"><c:out value="${allRegions[region]}"/></props:option>
      </c:forEach>
    </props:selectProperty>
    <span class="smallNote">Select the region where this connection will be used</span><span class="error" id="error_${region_name_param}"></span>
  </td>
</tr>

<props:selectSectionProperty name="${credentials_type_param}" title="${credentials_type_label}:" style="width: 20em;">
  <props:selectSectionPropertyContent value="${credentials_type_access_keys_option}" caption="${credentials_type_access_keys_label}">
    <jsp:include page="credentialTypeComponents/accessKeys/awsAccessKeysCredsComponent.jsp">
      <jsp:param name="connectionId" value="${oauthConnectionBean.getConnectionId()}"/>
    </jsp:include>
  </props:selectSectionPropertyContent>

  <props:selectSectionPropertyContent value="${credentials_type_iam_role_option}" caption="${credentials_type_iam_role_label}">
    <jsp:include page="credentialTypeComponents/iamRole/awsIamRoleCredsComponent.jsp"/>
  </props:selectSectionPropertyContent>

  <props:selectSectionPropertyContent value="${credentials_type_default_provider_option}" caption="${credentials_type_default_provider_label}">
    <jsp:include page="credentialTypeComponents/defaultCredsProvider/defaultCredsProviderComponent.jsp"/>
  </props:selectSectionPropertyContent>
</props:selectSectionProperty>

<c:set var="buildStepsFeatureEnabled" value="${project.parameters.getOrDefault(allowed_in_builds_feature_enabled, true)}"/>
<c:set var="subProjectsFeatureEnabled" value="${project.parameters.getOrDefault(allowed_in_subprojects_feature_enabled, true)}"/>

<c:if test="${buildStepsFeatureEnabled || subProjectsFeatureEnabled}">
  <l:settingsGroup title="Security">

    <c:if test="${buildStepsFeatureEnabled}">

      <c:set var="allowedInBuildsPropValue" value="${propertiesBean.properties[allowed_in_builds_param]}"/>
      <c:set var="allowedInBuildsValue" value="${empty connectionId ? 'false' : empty allowedInBuildsPropValue ? 'true' : allowedInBuildsPropValue}"/>
      <tr>
        <th><label for="${allowed_in_builds_param}">${allowed_in_builds_label}: </label></th>
        <td>
          <props:checkboxProperty name="${allowed_in_builds_param}" checked="${allowedInBuildsValue}" uncheckedValue="false"/>
          <span>${allowed_in_builds_note}</span>
          <span class="error" id="error_${allowed_in_builds_param}"></span>
        </td>
      </tr>
    </c:if>

    <c:set var="allowedInSubProjectsPropValue" value="${propertiesBean.properties[allowed_in_subprojects_param]}"/>
    <c:set var="allowedInSubProjectsValue" value="${empty connectionId ? 'false' : empty allowedInSubProjectsPropValue ? 'true' : allowedInSubProjectsPropValue}"/>

    <c:if test="${subProjectsFeatureEnabled}">
      <tr>
        <th><label for="${allowed_in_subprojects_param}">${allowed_in_subprojects_label}: </label></th>
        <td>
          <props:checkboxProperty name="${allowed_in_subprojects_param}" checked="${allowedInSubProjectsValue}" uncheckedValue="false"/>
          <span>${allowed_in_subprojects_note}</span>
          <span class="error" id="error_${allowed_in_subprojects_param}"></span>
        </td>
      </tr>
    </c:if>
  </l:settingsGroup>
</c:if>

<script>
  BS.OAuthConnectionDialog.submitTestConnection = function () {
    const enableForm = this.enable.bind(this);
    BS.PasswordFormSaver.save(this, '${testAwsConnectionControllerUrl}', OO.extend(BS.ErrorsAwareListener, {
      onFailedTestConnectionError: function (elem) {
        let text = "";
        if (elem.firstChild) {
          text = elem.firstChild.nodeValue;
        }
        text += "Running STS get-caller-identity...";
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

        let additionalInfo = "Running STS get-caller-identity...\n";;
        if (testConnectionResultNodes.length > 0) {
          additionalInfo += "Caller Identity:";
          const testConnectionResult = testConnectionResultNodes.item(0);
          additionalInfo += "\n Account ID: " + testConnectionResult.getAttribute('${aws_caller_identity_attr_account_id}');
          additionalInfo += "\n User ID: " + testConnectionResult.getAttribute('${aws_caller_identity_attr_user_id}');
          additionalInfo += "\n ARN: " + testConnectionResult.getAttribute('${aws_caller_identity_attr_user_arn}');
        } else {
          additionalInfo += "Could not get the Caller Identity information from the response.";
        }

        BS.TestConnectionDialog.show(true, additionalInfo, $('testConnectionButton'));
      }
    }));
    $j('.error').css({"word-break": "break-all"});
    return false;
  };

  const afterClose = BS.OAuthConnectionDialog.afterClose;
  BS.OAuthConnectionDialog.afterClose = function () {
    $j('#awsTestConnectionButton').remove();
    afterClose();
  };

  BS.OAuthConnectionDialog.addError = function (errorHTML, errorId){
    const target = document.getElementById('error_' + errorId);
    target.innerHTML = errorHTML;
  };
  BS.OAuthConnectionDialog.clearError = function (errorId){
    const target = document.getElementById('error_' + errorId);
    target.innerHTML = '';
  };
  BS.OAuthConnectionDialog.clearAllErrors = function (errorIdsArray){
    errorIdsArray.forEach(errorId => {
      this.clearError(errorId);
    })
  };
</script>

<forms:button id="awsTestConnectionButton" className="testConnectionButton"
              onclick="return BS.OAuthConnectionDialog.submitTestConnection();">Test Connection</forms:button>

<bs:dialog dialogId="testConnectionDialog" title="Test Connection" closeCommand="BS.TestConnectionDialog.close();"
           closeAttrs="showdiscardchangesmessage='false'">
  <div id="testConnectionStatus"></div>
  <div id="testConnectionDetails" class="mono"></div>
</bs:dialog>

<script>
  // duplication of awsTestConnectionButton can happen for some reason, when the page rendered twice.
  // check if the button is already moved
  if ($j('#OAuthConnectionDialog .popupSaveButtonsBlock .testConnectionButton').length == 0) {
    $j('#awsTestConnectionButton').insertAfter('#OAuthConnectionDialog .popupSaveButtonsBlock .cancel');
  } else if ($j('.testConnectionButton').length > 1) {
    // remove the duplicate button from the top of the dialog
    $j('.testConnectionButton').first().remove();
  }
</script>
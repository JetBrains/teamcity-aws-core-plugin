<%@ include file="/include-internal.jsp" %>
<%@include file="awsConnectionConstants.jspf" %>
<%@include file="credentialTypeComponents/accessKeys/awsAccessKeysCredsConstants.jspf" %>
<%@include file="credentialTypeComponents/iamRole/awsIamRoleCredsConstants.jspf" %>
<%@include file="credentialTypeComponents/defaultCredsProvider/defaultCredsProviderConstants.jspf" %>

<%@ taglib prefix="intprop" uri="/WEB-INF/functions/intprop" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="util" uri="/WEB-INF/functions/util" %>
<%@ taglib prefix="afn" uri="/WEB-INF/functions/authz" %>

<%@ page import="jetbrains.buildServer.util.StringUtil" %>
<%@ page import="jetbrains.buildServer.serverSide.crypt.RSACipher" %>
<%@ page import="jetbrains.buildServer.serverSide.oauth.aws.controllers.SupportedProvidersController" %>
<%@ page import="jetbrains.buildServer.clouds.amazon.connector.keyRotation.impl.OldKeysCleaner" %>

<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean" />
<jsp:useBean id="oauthConnectionBean" scope="request" type="jetbrains.buildServer.serverSide.oauth.OAuthConnectionBean" />
<jsp:useBean id="project" type="jetbrains.buildServer.serverSide.SProject" scope="request" />

<c:set var="isDefaultCredProviderEnabled" value="${intprop:getBoolean(default_creds_provider_prop_name)}" />
<c:set var="sessionCredentialsEnabled" value="${propertiesBean.properties[use_session_credentials_param]}" />
<c:set var="featureId" value="${propertiesBean.properties['id']}" />
<c:set var="iamRoleSessionName" value="${propertiesBean.properties[iam_role_session_name_param]}" />
<c:set var="iamRoleArn" value="${propertiesBean.properties[iam_role_arn_param]}" />
<c:set var="stsEndpoint" value="${propertiesBean.properties[sts_endpoint_param]}" />
<c:set var="secretAccessKey" value="${empty propertiesBean.properties[secure_secret_access_key_param] ? '' : propertiesBean.getEncryptedPropertyValue(secure_secret_access_key_param)}" />
<c:set var="accessKeyId" value="${propertiesBean.properties[access_key_id_param]}" />
<c:set var="credentialsType" value="${propertiesBean.properties[credentials_type_param]}" />
<c:set var="publicKey"><c:out value='<%=RSACipher.getHexEncodedPublicKey()%>' /></c:set>
<c:set var="region" value="${propertiesBean.properties[region_name_param]}" />
<c:set var="connectionId" value="${oauthConnectionBean.getConnectionId()}" />
<c:set var="projectId" value="${project.externalId}" />
<c:set var="displayName" value="${propertiesBean.properties[display_name_param]}" />
<c:set var="testConnectionUrl" value="<%=AwsCloudConnectorConstants.TEST_CONNECTION_CONTROLLER_URL%>" />
<c:set var="supportedProvidersUrl" value="<%=SupportedProvidersController.CONTROLLER_PATH%>" />
<c:set var="connectionsUrl" value="<%=AwsCloudConnectorConstants.AWS_CONNECTIONS_URL%>" />
<c:set var="externalIdsUrl" value="<%=AwsAssumeIamRoleParams.EXTERNAL_IDS_CONTROLLER_URL%>"/>
<c:set var="avail_connections_controller_url" value="${avail_connections_controller_url}" />
<c:set var="avail_connections_rest_resource_name" value="${avail_connections_rest_resource_name}"/>
<c:set var="awsConnIdRestParamForExternalIds" value="${aws_conn_id_rest_param}"/>
<c:set var="buildStepsFeatureEnabled" value="${project.parameters.getOrDefault(allowed_in_builds_feature_enabled, true)}" />
<c:set var="subProjectsFeatureEnabled" value="${project.parameters.getOrDefault(allowed_in_subprojects_feature_enabled, true)}" />
<c:set var="allowedInBuildsPropValue" value="${propertiesBean.properties[allowed_in_builds_param]}" />
<c:set var="allowedInBuildsValue" value="${empty connectionId ? 'false' : empty allowedInBuildsPropValue ? 'true' : allowedInBuildsPropValue}" />
<c:set var="allowedInSubProjectsPropValue" value="${propertiesBean.properties[allowed_in_subprojects_param]}" />
<c:set var="allowedInSubProjectsValue" value="${empty connectionId ? 'false' : empty allowedInSubProjectsPropValue ? 'true' : allowedInSubProjectsPropValue}" />
<c:set var="oldKeyPreserveTime" value="<%=OldKeysCleaner.getOldKeyPreserveTimeReadable()%>" />
<c:set var="awsConnectionId" value="${propertiesBean.properties[principal_aws_connection_param]}" />
<c:set var="canEditProject" value="${afn:permissionGrantedForProject(project, 'EDIT_PROJECT')}"/>
<c:set var="projectIsReadOnly" value="${project.readOnly}"/>

<%--All Regions--%>
<c:set var="allRegionKeys"
       value="${util:arrayToString(allRegions.keySet().toArray())}" />
<c:set var="allRegionValues"
       value="${util:arrayToString(allRegions.values().toArray())}" />

<div id="edit-aws-connection-root"></div>
<c:set var="frontendDefaultUrl"><c:url value='${teamcityPluginResourcesPath}bundle.js' /></c:set>
<c:set var="overrideBundleUrl" value="${intprop:getProperty('teamcity.plugins.SakuraUI-Plugin.aws.connection.bundleUrl', '')}" />

<c:set var="frontendCode">
  <c:choose>
    <c:when test="${!StringUtil.isEmpty(overrideBundleUrl)}">
      <c:out value="${overrideBundleUrl}/bundle.js" />
    </c:when>
    <c:otherwise>
      <c:out value='${frontendDefaultUrl}' />
    </c:otherwise>
  </c:choose>
</c:set>

<script type="text/javascript">
  {
    const allRegions = {
      allRegionKeys: "<bs:forJs>${allRegionKeys}</bs:forJs>",
      allRegionValues: "<bs:forJs>${allRegionValues}</bs:forJs>",
    };

    const config = {
      projectId: "<bs:forJs>${projectId}</bs:forJs>",
      connectionId: "<bs:forJs>${connectionId}</bs:forJs>",
      supportedProvidersUrl: "<bs:forJs>${supportedProvidersUrl}</bs:forJs>",
      connectionsUrl: "<bs:forJs>${connectionsUrl}</bs:forJs>",
      displayName: "<bs:forJs>${displayName}</bs:forJs>",
      region: "<bs:forJs>${region}</bs:forJs>",
      defaultRegion: "<bs:forJs>${region_name_default}</bs:forJs>",
      credentialsType: "<bs:forJs>${credentialsType}</bs:forJs>",
      accessKeyId: "<bs:forJs>${accessKeyId}</bs:forJs>",
      secretAccessKey: "<bs:forJs>${secretAccessKey}</bs:forJs>",
      sessionCredentialsEnabled: "<bs:forJs>${sessionCredentialsEnabled}</bs:forJs>",
      stsEndpoint: "<bs:forJs>${stsEndpoint}</bs:forJs>",
      iamRoleArn: "<bs:forJs>${iamRoleArn}</bs:forJs>",
      iamRoleSessionName: "<bs:forJs>${iamRoleSessionName}</bs:forJs>",
      buildStepsFeatureEnabled: "<bs:forJs>${buildStepsFeatureEnabled}</bs:forJs>" === "true",
      subProjectsFeatureEnabled: "<bs:forJs>${subProjectsFeatureEnabled}</bs:forJs>" === "true",
      allowedInBuildsValue: "<bs:forJs>${allowedInBuildsValue}</bs:forJs>" === "true",
      allowedInSubProjectsValue: "<bs:forJs>${allowedInSubProjectsValue}</bs:forJs>" === "true",
      publicKey: "<bs:forJs>${publicKey}</bs:forJs>",
      featureId: "<bs:forJs>${featureId}</bs:forJs>",
      testConnectionUrl: "<bs:forJs>${testConnectionUrl}</bs:forJs>",
      allRegions: allRegions,
      isDefaultCredProviderEnabled: "<bs:forJs>${isDefaultCredProviderEnabled}</bs:forJs>" === "true",
      availableAwsConnectionsControllerResource: "<bs:forJs>${avail_connections_rest_resource_name}</bs:forJs>",
      availableAwsConnectionsControllerUrl: "<bs:forJs>${avail_connections_controller_url}</bs:forJs>",
      externalIdsControllerUrl: "<bs:forJs>${externalIdsUrl}</bs:forJs>",
      externalIdsConnectionParam: "<bs:forJs>${awsConnIdRestParamForExternalIds}</bs:forJs>",
      awsConnectionId: "<bs:forJs>${awsConnectionId}</bs:forJs>",
      rotateKeyControllerUrl: "<bs:forJs>${rotate_key_controller_url}</bs:forJs>",
      readOnly: "<bs:forJs>${projectIsReadOnly || !canEditProject}</bs:forJs>" === "true",
      oldKeyPreserveTime: "<bs:forJs>${oldKeyPreserveTime}</bs:forJs>",
    };

    const loadJS = function (url, implementationCode, location) {
      const scriptTag = document.createElement('script');
      scriptTag.src = url;
      scriptTag.onload = implementationCode;
      location.appendChild(scriptTag);
    };

    const callback = function () {
      renderEditAwsConnection(config);
    };

    loadJS("<bs:forJs>${frontendCode}</bs:forJs>", callback, document.body);
  }
</script>



<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="intprop" uri="/WEB-INF/functions/intprop" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>
<jsp:useBean id="buildForm"  scope="request" type="jetbrains.buildServer.controllers.admin.projects.EditableBuildTypeSettingsForm"/>
<jsp:useBean id="buildFeature"  scope="request" type="jetbrains.buildServer.clouds.amazon.connector.featureDevelopment.credsToAgent.AwsConnToAgentBuildFeature"/>

<%@include file="awsConnToAgentBuildFeatureConstants.jspf" %>

<tr>
  <td colspan="2">
    <em>Select the AWS connection, which will be provided to a build agent. <bs:helpLink file="aws-credentials"><bs:helpIcon/></bs:helpLink></em>
  </td>
</tr>

<jsp:include page="${buildFeature.availAwsConnsUrl}"/>
<tr id="${aws_profile_name_param}_row">
  <th><label for="${aws_profile_name_param}">${aws_profile_name_label}: </label></th>
  <td><props:textProperty name="${aws_profile_name_param}" className="longField" maxlength="256" noAutoComplete="true"/>
    <span class="smallNote">Leave empty for [default]</span>
    <span class="error" id="error_${aws_profile_name_param}" style="word-break: break-all;"></span>
  </td>
</tr>
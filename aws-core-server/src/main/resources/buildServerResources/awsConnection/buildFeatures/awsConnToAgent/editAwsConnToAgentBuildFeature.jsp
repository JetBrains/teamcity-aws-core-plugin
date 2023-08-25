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


<jsp:useBean id="buildForm"  scope="request" type="jetbrains.buildServer.controllers.admin.projects.EditableBuildTypeSettingsForm"/>

<jsp:include page="availableAwsConnections.jsp">
  <jsp:param name="projectId" value="${buildForm.project.externalId}"/>
  <jsp:param name="sessionDuration" value="${param.sessionDuration}"/>
  <jsp:param name="forBuildStep" value="true"/>
</jsp:include>
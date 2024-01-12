

<jsp:useBean id="project" type="jetbrains.buildServer.serverSide.SProject" scope="request"/>

<jsp:include page="availableAwsConnections.jsp">
  <jsp:param name="projectId" value="${project.externalId}"/>
  <jsp:param name="sessionDuration" value="${param.sessionDuration}"/>
  <jsp:param name="forBuildStep" value="false"/>
</jsp:include>
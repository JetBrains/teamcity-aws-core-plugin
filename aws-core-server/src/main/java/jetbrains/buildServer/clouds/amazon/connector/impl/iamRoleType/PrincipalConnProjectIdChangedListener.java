package jetbrains.buildServer.clouds.amazon.connector.impl.iamRoleType;

import java.util.HashMap;
import java.util.Map;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsAssumeIamRoleParams;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.auth.AuthorityHolder;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager;
import jetbrains.buildServer.serverSide.oauth.OAuthConstants;
import jetbrains.buildServer.serverSide.oauth.aws.AwsConnectionProvider;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.util.EventDispatcher;
import org.jetbrains.annotations.NotNull;

public class PrincipalConnProjectIdChangedListener extends ConfigActionsServerAdapter {

  private final SecurityContextEx mySecurityContext;
  private final ConfigActionFactory myConfigActionFactory;
  private final OAuthConnectionsManager myOAuthConnectionsManager;

  public PrincipalConnProjectIdChangedListener(@NotNull EventDispatcher<ConfigActionsServerListener> configEvents,
                                               @NotNull final SecurityContextEx securityContext,
                                               @NotNull final ConfigActionFactory configActionFactory,
                                               @NotNull final OAuthConnectionsManager oAuthConnectionsManager) {
    mySecurityContext = securityContext;
    myConfigActionFactory = configActionFactory;
    myOAuthConnectionsManager = oAuthConnectionsManager;
    configEvents.addListener(this);
  }

  @Override
  public void projectExternalIdChanged(@NotNull final ConfigAction cause,
                                       @NotNull final SProject project,
                                       @NotNull final String oldExtId,
                                       @NotNull final String newId) {

    updateAwsPrincipalConnProjectId(project, newId);

    for (SProject p : project.getOwnProjects()) {
      if (p.isReadOnly()) continue;
      updateAwsPrincipalConnProjectId(project, newId);
    }
  }

  private void updateAwsPrincipalConnProjectId(@NotNull SProject project, @NotNull final String newExtId) {
    for (SProjectFeatureDescriptor connectionFeature : project.getOwnFeaturesOfType(OAuthConstants.FEATURE_TYPE)) {
      if (isAwsConnection(connectionFeature)) {
        Map<String, String> newParameters = new HashMap<>(connectionFeature.getParameters());
        newParameters.put(AwsAssumeIamRoleParams.PRINCIPAL_AWS_CONN_PROJECT_ID_PARAM, newExtId);

        myOAuthConnectionsManager.updateConnection(
          project,
          connectionFeature.getId(),
          AwsConnectionProvider.TYPE,
          newParameters
        );
        persist(project);
      }
    }
  }

  private boolean isAwsConnection(@NotNull final SProjectFeatureDescriptor connectionFeature) {
    String providerType = connectionFeature.getParameters().get(OAuthConstants.OAUTH_TYPE_PARAM);
    return AwsConnectionProvider.TYPE.equals(providerType);
  }

  private void persist(@NotNull final SProject project) {
    final AuthorityHolder authHolder = mySecurityContext.getAuthorityHolder();
    try {
      if (authHolder instanceof SUser) {
        project.schedulePersisting(myConfigActionFactory.createAction((SUser)authHolder, project, "Connection updated: Project External ID changed"));
      } else {
        project.schedulePersisting(myConfigActionFactory.createAction(project, "Connection updated: Project External ID changed"));
      }
    } catch (PersistFailedException e) {
      Loggers.SERVER.warnAndDebugDetails("Failed to persist project on disk: " + project, e);
    }
  }
}

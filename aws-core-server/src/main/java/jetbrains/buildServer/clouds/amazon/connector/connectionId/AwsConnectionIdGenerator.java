

package jetbrains.buildServer.clouds.amazon.connector.connectionId;

import com.intellij.openapi.diagnostic.Logger;
import java.util.Map;
import jetbrains.buildServer.clouds.amazon.connector.common.AwsConnectionsHolder;
import jetbrains.buildServer.serverSide.identifiers.BaseExternalIdGenerator;
import jetbrains.buildServer.serverSide.oauth.aws.AwsConnectionProvider;
import jetbrains.buildServer.serverSide.oauth.identifiers.OAuthConnectionsIdGenerator;
import jetbrains.buildServer.util.CachingTypedIdGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.USER_DEFINED_ID_PARAM;

public class AwsConnectionIdGenerator extends BaseExternalIdGenerator implements CachingTypedIdGenerator {
  public final static String ID_GENERATOR_TYPE = AwsConnectionProvider.TYPE;
  public final static String AWS_CONNECTION_ID_PREFIX = "awsConnection";
  public final static int INITIAL_AWS_CONNECTION_ID = 0;

  private final static Logger LOG = Logger.getInstance(AwsConnectionIdGenerator.class.getName());


  private final AwsConnectionsHolder myAwsConnectionsHolder;

  public AwsConnectionIdGenerator(@NotNull final AwsConnectionsHolder awsConnectionsHolder,
                                  @NotNull final OAuthConnectionsIdGenerator OAuthConnectionsIdGenerator) {
    myAwsConnectionsHolder = awsConnectionsHolder;

    OAuthConnectionsIdGenerator.registerProviderTypeGenerator(ID_GENERATOR_TYPE, this);
  }

  public static String formatId(@NotNull final String connectionId, int newIdNumber) {
    return String.format("%s_%s", connectionId, String.valueOf(newIdNumber));
  }

  @Nullable
  @Override
  public String newId(@NotNull Map<String, String> props) {
    String userDefinedConnId = props.get(USER_DEFINED_ID_PARAM);

    // try to remove it from the properties map to avoid it being persisted
    try {
      props.remove(USER_DEFINED_ID_PARAM);
    }catch (Throwable e){
      // Ignore exception until TW-78234 is fixed
    }

    if (userDefinedConnId == null) {
      userDefinedConnId = generateNewId();
      LOG.debug("User did not define the connection id, will generate it using awsConnection_<incremental ID>");
    } else if (!isUnique(userDefinedConnId)) {
      userDefinedConnId = makeUnique(userDefinedConnId);
      LOG.warn("User-defined connection id is not unique, will add incremental ID");
    }

    LOG.debug("Will use: \"" + userDefinedConnId + "\" as AWS Connection id");

    return userDefinedConnId;
  }

  @Nullable
  @Override
  public String showNextId(@NotNull Map<String, String> props) {
    return generateNewId();
  }

  @Override
  public void addGeneratedId(@NotNull final String id, @NotNull final Map<String, String> props) {
    //ignored, all AWS Connections IDs are handled by AwsConnectionsHandler via AwsConnectionsEventListener
  }

  public boolean isUnique(@NotNull final String connectionId) {
    return myAwsConnectionsHolder.isUniqueAwsConnectionId(connectionId);
  }

  @NotNull
  private String generateNewId() {
    return makeUnique(AWS_CONNECTION_ID_PREFIX);
  }

  @NotNull
  private synchronized String makeUnique(@NotNull final String userDefinedConnId) {
    int counter = INITIAL_AWS_CONNECTION_ID;
    String newAwsConnectionId;
    do {
      newAwsConnectionId = formatId(userDefinedConnId, ++counter);
    } while (!isUnique(newAwsConnectionId));
    return newAwsConnectionId;
  }

  @Override
  protected boolean isExternalIdOccupied(@NotNull String newExtId) {
    return !isUnique(newExtId);
  }
}
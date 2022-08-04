package jetbrains.buildServer.clouds.amazon.ami;

import java.util.HashMap;
import java.util.Map;
import jetbrains.buildServer.messages.serviceMessages.RemoteArtifactMessage;
import org.jetbrains.annotations.NotNull;

import static jetbrains.buildServer.clouds.amazon.ami.AmiConstants.*;

/**
 * Helper class that allows to create a service message for publishing an EC2 AMIs as remote artifact
 *
 * @since 2022.10
 */
public class AMIArtifactMessage extends RemoteArtifactMessage {

  private AMIArtifactMessage(@NotNull Map<String, String> attributes) {
    super(attributes);
  }

  /**
   * Helper method for creating AmiArtifactMessage
   *
   * @param amiId                - ID of created AMI
   * @param connectionId         - ID of AWS connection that can be used to access that AMI
   * @param connectionParameters - additional parameters that are needed to use specified AWS connection(session duration etc.)
   * @return - {#{@link RemoteArtifactMessage}} of specific type that describes an EC2 AMI
   */
  @NotNull
  public static AMIArtifactMessage create(@NotNull String amiId, @NotNull String connectionId, @NotNull Map<String, String> connectionParameters) {
    final HashMap<String, String> attributes = new HashMap<>();

    attributes.put(RemoteArtifactMessage.TYPE_ATTRIBUTE, ARTIFACT_TYPE);
    attributes.put(PARAM_AMI_ID, amiId);
    attributes.put(PARAM_CONNECTION_ID, connectionId);
    attributes.putAll(connectionParameters);

    return new AMIArtifactMessage(attributes);
  }
}

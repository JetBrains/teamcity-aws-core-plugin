package jetbrains.buildServer.clouds.amazon.ami;

import java.util.Map;
import jetbrains.buildServer.serverSide.artifacts.RemoteArtifact;
import org.jetbrains.annotations.NotNull;

import static jetbrains.buildServer.clouds.amazon.ami.AmiConstants.*;

/**
 * Helper class to simplify working with remote artifacts for EC2 AMIs
 *
 * @since 2022.10
 */
public class AmiArtifact extends RemoteArtifact {
  public AmiArtifact(@NotNull Map<String, String> attributes) {
    super(ARTIFACT_TYPE, validatedAttributes(attributes));
  }

  @NotNull
  private static Map<String, String> validatedAttributes(@NotNull Map<String, String> attributes) {
    if (!attributes.containsKey(PARAM_AMI_ID)) {
      throw new IllegalArgumentException("Should contain " + PARAM_AMI_ID);
    }
    if (!attributes.containsKey(PARAM_CONNECTION_ID)) {
      throw new IllegalArgumentException("Should contain " + PARAM_CONNECTION_ID);
    }

    return attributes;
  }

  /**
   * @return ID of an AWS connection that can be used to access this AMI
   */
  @NotNull
  public String getConnectionId() {
    return getAttributes().get(PARAM_CONNECTION_ID);
  }

  /**
   * @return ID of an AMI
   */
  @NotNull
  public String getAmiId() {
    return getAttributes().get(PARAM_AMI_ID);
  }

}

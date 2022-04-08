package jetbrains.buildServer.clouds.amazon.connector.impl.dataBeans;

import java.util.HashMap;
import java.util.Map;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.serverSide.oauth.aws.AwsConnectionProvider;
import org.jetbrains.annotations.NotNull;

import jetbrains.buildServer.controllers.BasePropertiesBean;

public class AwsConnectionBean extends BasePropertiesBean {
  private String myConnectionId = "";
  private String myProviderType = "";

  public AwsConnectionBean() {
    super(new HashMap<String, String>(), new HashMap<String, String>());
  }

  public AwsConnectionBean(@NotNull final Map<String, String> properties, @NotNull final Map<String, String> defaultProperties) {
    super(properties, defaultProperties);
  }

  public AwsConnectionBean(@NotNull final OAuthConnectionDescriptor connectionDescriptor) {
    super(new HashMap<String, String>(), new HashMap<String, String>());
    if(connectionDescriptor.getOauthProvider() instanceof AwsConnectionProvider){
      setConnectionId(connectionDescriptor.getId());
      setProviderType(connectionDescriptor.getOauthProvider().getType());
      setProperties(connectionDescriptor.getParameters());
    }
  }

  @NotNull
  public String getConnectionId() {
    return myConnectionId;
  }

  public void setConnectionId(@NotNull final  String connectionId) {
    myConnectionId = connectionId;
  }

  @NotNull
  public String getProviderType() {
    return myProviderType;
  }

  public void setProviderType(@NotNull final  String providerType) {
    myProviderType = providerType;
  }
}



package jetbrains.buildServer.util.amazon;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import java.util.Map;
import jetbrains.buildServer.Used;
import jetbrains.buildServer.util.CollectionsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author vbedrosova
 */
public class AWSException extends RuntimeException {
  @NotNull
  private static final String NETWORK_PROBLEM_MESSAGE = "Unable to execute HTTP request";
  @Used("CodeDeploy")
  @NotNull
  public static String SERVICE_PROBLEM_TYPE = "AWS_SERVICE";
  @Used("CodeDeploy")
  @NotNull
  public static String CLIENT_PROBLEM_TYPE = "AWS_CLIENT";
  @Used("CodeDeploy")
  @NotNull
  public static String EXCEPTION_BUILD_PROBLEM_TYPE = "AWS_EXCEPTION";
  @NotNull
  @Used("CodeDeploy")
  public static Map<String, String> PROBLEM_TYPES = CollectionsUtil.asMap(
    SERVICE_PROBLEM_TYPE, "Amazon service exception",
    CLIENT_PROBLEM_TYPE, "Amazon client exception",
    EXCEPTION_BUILD_PROBLEM_TYPE, "Amazon unexpected exception");
  @Nullable
  private final String myIdentity;
  @NotNull
  private final String myType;
  @Nullable
  private final String myDetails;

  @Used("CodeDeploy")
  public AWSException(@NotNull String message, @Nullable String identity, @NotNull String type, @Nullable String details) {
    super(message);
    myIdentity = identity;
    myType = type;
    myDetails = details;
  }

  public AWSException(@NotNull Throwable t) {
    super(getMessage(t), t);
    myIdentity = getIdentity(t);
    myType = getType(t);
    myDetails = getDetails(t);
  }

  @NotNull
  public static String getMessage(@NotNull Throwable t) {
    if (t instanceof AWSException) return t.getMessage();
    if (t instanceof AmazonServiceException)  return "AWS error: " + removeTrailingDot(((AmazonServiceException) t).getErrorMessage());
    if (t instanceof AmazonClientException) {
      final String message = t.getMessage();
      if (message.contains(NETWORK_PROBLEM_MESSAGE)) {
        return "Unable to access AWS. Check your network settings and try again.";
      } else {
        return "AWS client error: " + removeTrailingDot(message);
      }
    }
    return "Unexpected error: " + removeTrailingDot(t.getMessage());
  }

  @Used("CodeDeploy")
  @Nullable
  public static String getIdentity(@NotNull Throwable t) {
    if (t instanceof AWSException) return ((AWSException) t).getIdentity();
    if (t instanceof AmazonServiceException) {
      final AmazonServiceException ase = (AmazonServiceException) t;
      return ase.getServiceName() + ase.getErrorType().name() + ase.getStatusCode() + ase.getErrorCode();
    }
    return null;
  }

  @NotNull
  public static String getType(@NotNull Throwable t) {
    if (t instanceof  AWSException) return ((AWSException) t).getType();
    if (t instanceof AmazonServiceException) return SERVICE_PROBLEM_TYPE;
    if (t instanceof AmazonClientException) return CLIENT_PROBLEM_TYPE;
    return EXCEPTION_BUILD_PROBLEM_TYPE;
  }

  @Used("CodeDeploy")
  @Nullable
  public static String getDetails(@NotNull Throwable t) {
    if (t instanceof AWSException) return ((AWSException) t).getDetails();
    if (t instanceof AmazonServiceException) {
      final AmazonServiceException ase = (AmazonServiceException) t;
      return "\n" +
        "Service:             " + ase.getServiceName() + "\n" +
        "HTTP Status Code:    " + ase.getStatusCode() + "\n" +
        "AWS Error Code:      " + ase.getErrorCode() + "\n" +
        "Error Type:          " + ase.getErrorType() + "\n" +
        "Request ID:          " + ase.getRequestId();
    }
    return null;
  }

  @Nullable
  private static String removeTrailingDot(@Nullable String msg) {
    return (msg != null && msg.endsWith(".")) ? msg.substring(0, msg.length() - 1) : msg;
  }

  @Used("CodeDeploy")
  @NotNull
  public String getIdentity() {
    return myIdentity == null ? getMessage() :  myIdentity;
  }

  @NotNull
  public String getType() {
    return myType;
  }

  @Nullable
  public String getDetails() {
    return myDetails;
  }
}
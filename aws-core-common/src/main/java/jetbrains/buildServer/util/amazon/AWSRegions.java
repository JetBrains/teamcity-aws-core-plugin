

package jetbrains.buildServer.util.amazon;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author vbedrosova
 */
@Deprecated
public final class AWSRegions {
  private static final Map<String, String> REGION_NAMES_FOR_WEB;

  static {
    REGION_NAMES_FOR_WEB = new LinkedHashMap<String, String>();
    REGION_NAMES_FOR_WEB.put("us-east-1", "US East (N. Virginia)");
    REGION_NAMES_FOR_WEB.put("us-east-2", "US East (Ohio)");
    REGION_NAMES_FOR_WEB.put("us-west-1", "US West (N. California)");
    REGION_NAMES_FOR_WEB.put("us-west-2", "US West (Oregon)");
    REGION_NAMES_FOR_WEB.put("ca-central-1", "Canada (Central)");
    REGION_NAMES_FOR_WEB.put("eu-north-1", "EU North (Stockholm)");
    REGION_NAMES_FOR_WEB.put("eu-west-1", "EU West (Dublin)");
    REGION_NAMES_FOR_WEB.put("eu-west-2", "EU West (London)");
    REGION_NAMES_FOR_WEB.put("eu-west-3", "EU West (Paris)");
    REGION_NAMES_FOR_WEB.put("eu-central-1", "EU Central (Frankfurt)");
    REGION_NAMES_FOR_WEB.put("sa-east-1", "South America (Sao Paulo)");
    REGION_NAMES_FOR_WEB.put("me-south-1", "Middle East (Bahrain)");
    REGION_NAMES_FOR_WEB.put("ap-south-1", "Asia Pacific (Mumbai)");
    REGION_NAMES_FOR_WEB.put("ap-northeast-1", "Asia Pacific (Tokyo)");
    REGION_NAMES_FOR_WEB.put("ap-northeast-2", "Asia Pacific (Seoul)");
    REGION_NAMES_FOR_WEB.put("ap-southeast-1", "Asia Pacific (Singapore)");
    REGION_NAMES_FOR_WEB.put("ap-southeast-2", "Asia Pacific (Sydney)");
    REGION_NAMES_FOR_WEB.put("ap-east-1", "Asia Pacific (Hong Kong)");
    REGION_NAMES_FOR_WEB.put("us-gov-west-1", "AWS GovCloud (US)");
    REGION_NAMES_FOR_WEB.put("us-gov-east-1", "AWS GovCloud (US-East)");
    REGION_NAMES_FOR_WEB.put("us-iso-east-1", "US ISO East");
    REGION_NAMES_FOR_WEB.put("us-isob-east-1", "US ISOB East (Ohio)");
    REGION_NAMES_FOR_WEB.put("cn-north-1", "China (Beijing)");
    REGION_NAMES_FOR_WEB.put("cn-northwest-1", "China (Ningxia)");

    for (Regions region : Regions.values()) {
      if (REGION_NAMES_FOR_WEB.containsKey(region.getName())) continue;
      REGION_NAMES_FOR_WEB.put(region.getName(), region.getDescription());
    }
  }

  public static String DEFAULT_REGION = "us-east-1";

  @NotNull
  public static String getRegionNameForWeb(@NotNull String regionCode){
    final String niceName = REGION_NAMES_FOR_WEB.get(regionCode);
    return niceName == null ? regionCode : niceName;
  }

  @NotNull
  public static Map<String,String> getAllRegions(){
    return Collections.unmodifiableMap(REGION_NAMES_FOR_WEB);
  }

  @NotNull
  public static Region getRegion(@NotNull String regionName) throws IllegalArgumentException {
    try {
      return Region.getRegion(Regions.fromName(regionName));
    } catch (Exception e) {
      // see below
    }
    throw new IllegalArgumentException("Unsupported region name " + regionName);
  }
}
package jetbrains.buildServer.clouds.amazon.connector.utils.parameters.regions;

import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.RegionMetadata;
import software.amazon.awssdk.regions.ServiceMetadata;
import software.amazon.awssdk.regions.internal.MetadataLoader;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author vbedrosova
 */
public final class AWSRegions {

  private static final String REGEX_SEPARATOR = "|";
  private static final String SPECIAL_DESIGNATION_REGIONS_PREFIX = Stream.of(
      RegionSortPriority.US_GOV,
      RegionSortPriority.US_ISO,
      RegionSortPriority.US_ISOB,
      RegionSortPriority.EU_ISOE)
    .map(RegionSortPriority::getPrefix)
    .collect(Collectors.joining(REGEX_SEPARATOR));
  private static final Pattern SPECIAL_DESIGNATION_PATTERN = Pattern.compile("^(" + SPECIAL_DESIGNATION_REGIONS_PREFIX + ")-.*$");
  private static final char REGION_SEPARATOR = '-';
  private static final Comparator<String> REGION_COMPARATOR = Comparator.comparingInt(AWSRegions::getRegionalPriority)
    .thenComparing(Comparator.naturalOrder());
  private static final Map<String, String> ADDITIONAL_DESCRIPTIONS;

  private static int getRegionalPriority(String region) {
    String prefix;

    if (SPECIAL_DESIGNATION_PATTERN.matcher(region).matches()) {
      int firstSeparatorIndex = region.indexOf(REGION_SEPARATOR);
      prefix = region.substring(0, region.indexOf('-', firstSeparatorIndex + 1));
    } else {
      prefix = region.substring(0, region.indexOf(REGION_SEPARATOR));
    }

    return RegionSortPriority.getPriority(prefix);
  }

  @SuppressWarnings("FieldCanBeLocal")
  private static final Map<String, Map<String, String>> REGIONS_DATA_BY_SERVICE;
  private static final String SERIALIZED_REGION_CODES;
  private static final String SERIALIZED_REGION_DESCRIPTIONS;
  public static String DEFAULT_REGION = "us-east-1";

  public static final String NO_SERVICE = "default";

  static {
    ADDITIONAL_DESCRIPTIONS = new HashMap<>();
    ADDITIONAL_DESCRIPTIONS.put("us-east-1", "US East (N. Virginia)");
    ADDITIONAL_DESCRIPTIONS.put("us-east-2", "US East (Ohio)");
    ADDITIONAL_DESCRIPTIONS.put("us-west-1", "US West (N. California)");
    ADDITIONAL_DESCRIPTIONS.put("us-west-2", "US West (Oregon)");
    ADDITIONAL_DESCRIPTIONS.put("ca-central-1", "Canada (Central)");
    ADDITIONAL_DESCRIPTIONS.put("ca-west-1", "Canada (West)");
    ADDITIONAL_DESCRIPTIONS.put("eu-west-1", "EU West (Dublin)");
    ADDITIONAL_DESCRIPTIONS.put("eu-west-2", "EU West (London)");
    ADDITIONAL_DESCRIPTIONS.put("eu-west-3", "EU West (Paris)");
    ADDITIONAL_DESCRIPTIONS.put("eu-central-1", "EU Central (Frankfurt)");
    ADDITIONAL_DESCRIPTIONS.put("eu-central-2", "Europe (Zurich)");
    ADDITIONAL_DESCRIPTIONS.put("eu-north-1", "EU North (Stockholm)");
    ADDITIONAL_DESCRIPTIONS.put("sa-east-1", "South America (Sao Paulo)");
    ADDITIONAL_DESCRIPTIONS.put("ap-northeast-1", "Asia Pacific (Tokyo)");
    ADDITIONAL_DESCRIPTIONS.put("ap-northeast-2", "Asia Pacific (Seoul)");
    ADDITIONAL_DESCRIPTIONS.put("ap-northeast-3", "Asia Pacific (Osaka)");
    ADDITIONAL_DESCRIPTIONS.put("ap-southeast-1", "Asia Pacific (Singapore)");
    ADDITIONAL_DESCRIPTIONS.put("ap-southeast-2", "Asia Pacific (Sydney)");
    ADDITIONAL_DESCRIPTIONS.put("ap-southeast-3", "Asia Pacific (Jakarta)");
    ADDITIONAL_DESCRIPTIONS.put("ap-southeast-4", "Asia Pacific (Melbourne)");
    ADDITIONAL_DESCRIPTIONS.put("ap-south-1", "Asia Pacific (Mumbai)");
    ADDITIONAL_DESCRIPTIONS.put("ap-south-2", "Asia Pacific (Hyderabad)");
    ADDITIONAL_DESCRIPTIONS.put("us-gov-west-1", "AWS GovCloud (US)");
    ADDITIONAL_DESCRIPTIONS.put("us-gov-east-1", "AWS GovCloud East (US)");
    ADDITIONAL_DESCRIPTIONS.put("cn-north-1", "China (Beijing)");
    ADDITIONAL_DESCRIPTIONS.put("cn-northwest-1", "China (Ningxia)");
    ADDITIONAL_DESCRIPTIONS.put("me-south-1", "Middle East (Bahrain)");
    ADDITIONAL_DESCRIPTIONS.put("me-central-1", "Middle East (UAE)");
    ADDITIONAL_DESCRIPTIONS.put("us-iso-east-1", "US ISO East");
    ADDITIONAL_DESCRIPTIONS.put("us-isob-east-1", "US ISOB East (Ohio)");
    ADDITIONAL_DESCRIPTIONS.put("ap-east-1", "Asia Pacific (Hong Kong)");
    ADDITIONAL_DESCRIPTIONS.put("af-south-1", "Africa South (Cape Town)");
    ADDITIONAL_DESCRIPTIONS.put("eu-south-1", "EU South (Milan)");
    ADDITIONAL_DESCRIPTIONS.put("eu-south-2", "Europe (Spain)");
    ADDITIONAL_DESCRIPTIONS.put("us-iso-west-1", "US ISO West");
    ADDITIONAL_DESCRIPTIONS.put("il-central-1", "Israel (Tel Aviv)");
    ADDITIONAL_DESCRIPTIONS.put("eu-isoe-west-1", "AWS European Sovereign Cloud ISO West");

    final TreeMap<String, String> map = getRegionsForService(null);
    REGIONS_DATA_BY_SERVICE = new HashMap<>();
    REGIONS_DATA_BY_SERVICE.put(NO_SERVICE, map);

    SERIALIZED_REGION_CODES = Arrays.toString(map.keySet().toArray());
    SERIALIZED_REGION_DESCRIPTIONS = Arrays.toString(map.values().toArray());
  }

  /**
   * @param servicePrefix - The service endpoint prefix which can be retrieved from the constant ENDPOINT_PREFIX of the specific servicePrefix client interface, e.g. AmazonEC2.ENDPOINT_PREFIX or null for a default list of regions
   * @return - Map of region codes to region descriptions for a specific service or not tied to a specific service
   */
  @NotNull
  private static TreeMap<String, String> getRegionsForService(@Nullable String servicePrefix) {
    Map<String, String> allRegionsMap = Region.regions()
      .stream()
      .map(Region::metadata)
      .collect(Collectors.toMap(RegionMetadata::id, RegionMetadata::description));

    TreeMap<String, String> map = new TreeMap<>(REGION_COMPARATOR);
    ServiceMetadata serviceMetadata = MetadataLoader.serviceMetadata(servicePrefix);

    final List<Region> regions = serviceMetadata == null ?
      servicePrefix == null ?
        Region.regions() : Collections.emptyList()
      : serviceMetadata.regions();

    for (Region region : regions) {
      String name = region.id();
      String value = allRegionsMap.get(name);
      if (value == null) {
        value = descriptionFromCode(name);
      }
      map.putIfAbsent(name, value);
    }
    return map;
  }

  @NotNull
  public static String descriptionFromCode(@NotNull String code) {
    if (ADDITIONAL_DESCRIPTIONS.get(code) != null) {
      return ADDITIONAL_DESCRIPTIONS.get(code);
    }

    String[] split = code.split("-");
    StringBuilder result = new StringBuilder();
    int restIndex = 0;
    String regionCode = "";
    String regionName = null;
    for (int i = 0; i < split.length; i++) {
      regionCode = regionCode.isEmpty() ? split[i] : regionCode + "-" + split[i];
      restIndex = i + 1;
      final RegionSortPriority sortPriority = RegionSortPriority.getFromPrefix(regionCode);
      if (sortPriority != null) {
        regionName = sortPriority.getName();
        break;
      }
    }
    if (regionName != null) {
      result.append(regionName);
    } else {
      restIndex = 0;
    }

    if (restIndex < split.length) {
      for (int i = restIndex; i < split.length; i++) {
        result.append(" ").append(StringUtil.capitalize(split[i]));
      }
    }

    return result.toString().trim();
  }

  @NotNull
  public static Map<String, String> getAllRegions() {
    return Collections.unmodifiableMap(REGIONS_DATA_BY_SERVICE.get(NO_SERVICE));
  }

  @NotNull
  public static Map<String, String> getAllRegions(@Nullable String service) {
    if (service == null) {
      return getAllRegions();
    }
    Map<String, String> regionsForService = REGIONS_DATA_BY_SERVICE.get(service);
    if (regionsForService == null) {
      regionsForService = getRegionsForService(service);
      REGIONS_DATA_BY_SERVICE.put(service, regionsForService);
    }
    return Collections.unmodifiableMap(regionsForService);
  }

  @NotNull
  public static Region getRegion(@NotNull String regionName) throws IllegalArgumentException {
    if (getAllRegions().containsKey(regionName)) {
      return Region.of(regionName);
    }

    throw new IllegalArgumentException("Unsupported region name " + regionName);
  }

  @SuppressWarnings("unused")
  public static String getSerializedRegionCodes() {
    return SERIALIZED_REGION_CODES;
  }

  @SuppressWarnings("unused")
  public static String getSerializedRegionDescriptions() {
    return SERIALIZED_REGION_DESCRIPTIONS;
  }

  public static boolean isChinaRegion(@Nullable final String regionName) {
    if (regionName == null) {
      return false;
    }

    return regionName.startsWith(RegionSortPriority.CN.getPrefix());
  }
}

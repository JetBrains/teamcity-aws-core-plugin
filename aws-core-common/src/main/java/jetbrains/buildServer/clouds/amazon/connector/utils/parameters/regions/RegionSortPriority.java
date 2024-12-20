package jetbrains.buildServer.clouds.amazon.connector.utils.parameters.regions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum RegionSortPriority {
  US("us", "US", 10),
  EU("eu", "EU", 20),
  CA("ca", "Canada", 30),
  AP("ap", "Asia Pacific",40),
  SA("sa", "South America",50),
  DEFAULT("default", "Default", 55),
  CN("cn", "China", 60),
  US_ISO("us-iso", "US ISO", 70),
  EU_ISOE("eu-isoe", "AWS European Sovereign Cloud ISO", 75),
  US_ISOB("us-isob","US ISOB", 80),
  US_GOV("us-gov", "GovCloud", 90);

  private static final Map<String, RegionSortPriority> PREFIX_TO_REGION_SORT_PRIORITY =
    Arrays.stream(RegionSortPriority.values())
      .collect(Collectors.toMap(k -> k.myPrefix, Function.identity()));

  private final String myPrefix;
  private final String myName;
  private final int myPriority;

  RegionSortPriority(@NotNull final String prefix, final String name, final int priority) {
    myPrefix = prefix;
    myName = name;
    myPriority = priority;
  }

  public String getPrefix() {
    return myPrefix;
  }

  public int getPriority() {
    return myPriority;
  }

  public String getName(){
    return myName;
  }

  public static int getPriority(@NotNull final String prefix){
    return PREFIX_TO_REGION_SORT_PRIORITY.getOrDefault(prefix, RegionSortPriority.DEFAULT).getPriority();
  }

  @Nullable
  public static RegionSortPriority getFromPrefix(@NotNull final String prefix) {
    return PREFIX_TO_REGION_SORT_PRIORITY.get(prefix);
  }
}

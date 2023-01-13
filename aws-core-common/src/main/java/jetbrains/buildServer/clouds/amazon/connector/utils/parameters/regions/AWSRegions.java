/*
 * Copyright 2000-2022 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.clouds.amazon.connector.utils.parameters.regions;

import com.amazonaws.regions.Regions;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author vbedrosova
 */
public final class AWSRegions {
  private static final Map<String, String> REGION_NAMES_FOR_WEB;
  private static final Map<String, String> CHINA_REGION_NAMES_FOR_WEB;

  static {
    REGION_NAMES_FOR_WEB = new LinkedHashMap<String, String>();
    CHINA_REGION_NAMES_FOR_WEB = new LinkedHashMap<String, String>();

    REGION_NAMES_FOR_WEB.put("us-east-1", "US East (N. Virginia)");
    REGION_NAMES_FOR_WEB.put("us-east-2", "US East (Ohio)");
    REGION_NAMES_FOR_WEB.put("us-west-1", "US West (N. California)");
    REGION_NAMES_FOR_WEB.put("us-west-2", "US West (Oregon)");
    REGION_NAMES_FOR_WEB.put("ca-central-1", "Canada (Central)");
    REGION_NAMES_FOR_WEB.put("eu-north-1", "EU North (Stockholm)");
    REGION_NAMES_FOR_WEB.put("eu-west-1", "EU West (Ireland)");
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

    CHINA_REGION_NAMES_FOR_WEB.put("cn-north-1", "China (Beijing)");
    CHINA_REGION_NAMES_FOR_WEB.put("cn-northwest-1", "China (Ningxia)");

    for (Regions region : Regions.values()) {
      if (REGION_NAMES_FOR_WEB.containsKey(region.getName())) continue;
      REGION_NAMES_FOR_WEB.put(region.getName(), region.getDescription());
    }
  }

  @NotNull
  public static Map<String, String> getAllRegions() {
    return Collections.unmodifiableMap(REGION_NAMES_FOR_WEB);
  }

  public static boolean isChinaRegion(@Nullable final String regionName) {
    if (regionName == null)
      return false;

    return CHINA_REGION_NAMES_FOR_WEB.keySet().contains(regionName);
  }
}

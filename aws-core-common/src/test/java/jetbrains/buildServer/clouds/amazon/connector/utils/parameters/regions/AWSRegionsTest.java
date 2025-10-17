/*
 * Copyright 2000-2024 JetBrains s.r.o.
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

import org.testng.annotations.Test;
import software.amazon.awssdk.services.ec2.Ec2Client;

import static org.junit.Assert.*;

public class AWSRegionsTest {

  /**
   * Test case for descriptionFromCode for a known region code with an additional description.
   */
  @Test
  public void testDescriptionFromCode_withKnownRegionCode() {
    // Arrange
    String regionCode = "us-east-1";

    // Act
    String description = AWSRegions.descriptionFromCode(regionCode);

    // Assert
    assertNotNull(description);
    assertEquals("US East (N. Virginia)", description);
  }

  /**
   * Test case for descriptionFromCode with an unknown region code that has three parts.
   */
  @Test
  public void testDescriptionFromCode_withThreePartUnknownRegionCode() {
    // Arrange
    String regionCode = "us-new-1";

    // Act
    String description = AWSRegions.descriptionFromCode(regionCode);

    // Assert
    assertNotNull(description);
    assertEquals("US New 1", description);
  }

  /**
   * Test case for descriptionFromCode with an unknown region code that has four parts.
   */
  @Test
  public void testDescriptionFromCode_withFourPartUnknownRegionCode() {
    // Arrange
    String regionCode = "us-new-test-1";

    // Act
    String description = AWSRegions.descriptionFromCode(regionCode);

    // Assert
    assertNotNull(description);
    assertEquals("US New Test 1", description);
  }

  /**
   * Test case for descriptionFromCode with an unknown region code that does not match the format.
   */
  @Test
  public void testDescriptionFromCode_withUnknownRegionCodeInvalidFormat() {
    // Arrange
    String regionCode = "unknown-region-code";

    // Act
    String description = AWSRegions.descriptionFromCode(regionCode);

    // Assert
    assertNotNull(description);
    assertEquals("Unknown Region Code", description);
  }

  /**
   * Test case for descriptionFromCode with a special designation region code.
   */
  @Test
  public void testDescriptionFromCode_withSpecialDesignationRegionCode() {
    // Arrange
    String regionCode = "us-gov-west-1";

    // Act
    String description = AWSRegions.descriptionFromCode(regionCode);

    // Assert
    assertNotNull(description);
    assertEquals("AWS GovCloud (US)", description);
  }

  @Test
  public void testGetAllRegions_withoutService(){
    assertNotEquals(0, AWSRegions.getAllRegions().size());
  }

  @Test
  public void testGetAllRegions_withKnownService(){
    assertNotEquals(0, AWSRegions.getAllRegions(Ec2Client.SERVICE_NAME).size());
  }

  @Test
  public void testGetAllRegions_withUnknownService(){
    assertEquals(0, AWSRegions.getAllRegions("unknown").size());
  }
}

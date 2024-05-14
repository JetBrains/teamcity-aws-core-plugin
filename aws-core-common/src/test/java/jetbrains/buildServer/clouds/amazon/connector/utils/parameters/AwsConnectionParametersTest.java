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

package jetbrains.buildServer.clouds.amazon.connector.utils.parameters;

import org.assertj.core.api.Assertions;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class AwsConnectionParametersTest {

  @Test(dataProvider = "params")
  void testConnectionValidity(String[] params) {
    AwsConnectionParameters parameters = AwsConnectionParameters.AwsConnectionParametersBuilder
      .of(params[0])
      .withInternalProjectId(params[1])
      .build();

    boolean actual = parameters.hasConnectionIdentity();
    boolean expected = params[0] != null && params[1] != null;

    Assertions.assertThat(actual).isEqualTo(expected);
  }

  @DataProvider(name = "params")
  public static Object[][] params() {
    return new String[][] {
      new String[] {null, null},
      new String[] {"1", null},
      new String[] {null, "1"},
      new String[] {"1", null},
      new String[] {"1", "2"}
    };
  }

}

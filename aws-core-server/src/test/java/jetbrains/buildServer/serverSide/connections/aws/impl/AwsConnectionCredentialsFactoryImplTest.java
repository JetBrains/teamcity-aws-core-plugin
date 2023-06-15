/*
 * Copyright 2000-2023 JetBrains s.r.o.
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

package jetbrains.buildServer.serverSide.connections.aws.impl;

import jetbrains.buildServer.ExtendableServiceLocator;
import jetbrains.buildServer.clouds.amazon.connector.AwsConnectorFactory;
import jetbrains.buildServer.clouds.amazon.connector.impl.staticType.StaticCredentialsBuilder;
import jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentialsException;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.List;

import static jetbrains.buildServer.clouds.amazon.connector.utils.parameters.AwsCloudConnectorConstants.CREDENTIALS_TYPE_PARAM;
import static jetbrains.buildServer.testUtils.TestUtils.getStsClientProviderWithNoKeys;
import static org.testng.Assert.assertTrue;

public class AwsConnectionCredentialsFactoryImplTest {

  private AwsConnectionCredentialsFactoryImpl myAwsConnectorFactory;

  @BeforeMethod
  public void setup() {
    myAwsConnectorFactory = new AwsConnectionCredentialsFactoryImpl(Mockito.mock(ExtendableServiceLocator.class));
  }

  @Test
  public void givenAwsConnFactory_withUnknownCredentialsType_thenReturnInvalidPropsWithCredsTypeError() {
    List<InvalidProperty> invalidProperties = myAwsConnectorFactory.getInvalidProperties(
      Collections.singletonMap(AwsCloudConnectorConstants.CREDENTIALS_TYPE_PARAM, "UNKNOWN")
    );

    assertTrue(invalidProperties.contains(
      new InvalidProperty(
        CREDENTIALS_TYPE_PARAM,
        "The credentials type " + "UNKNOWN" + " is not supported."
      )
    ));
  }

  @Test
  public void givenAwsConnFactory_withoutCredsTypeProp_thenReturnInvalidPropsWithCredsTypeError() {
    List<InvalidProperty> invalidProperties = myAwsConnectorFactory.getInvalidProperties(
      Collections.emptyMap()
    );

    assertTrue(invalidProperties.contains(
      new InvalidProperty(
        CREDENTIALS_TYPE_PARAM,
        "The credentials type " + "null" + " is not supported."
      )
    ));
  }

  @Test(expectedExceptions = {IllegalStateException.class})
  public void givenAwsConnBuilderWithRegisteredFactory_whenTryingToRegisterTheSameType_thenThrowException() throws ConnectionCredentialsException {
    new StaticCredentialsBuilder(Mockito.mock(AwsConnectorFactory.class), myAwsConnectorFactory, getStsClientProviderWithNoKeys());
    new StaticCredentialsBuilder(Mockito.mock(AwsConnectorFactory.class), myAwsConnectorFactory, getStsClientProviderWithNoKeys());
  }
}
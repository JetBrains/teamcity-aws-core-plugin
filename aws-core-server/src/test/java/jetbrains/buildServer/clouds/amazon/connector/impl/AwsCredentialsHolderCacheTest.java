package jetbrains.buildServer.clouds.amazon.connector.impl;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import jetbrains.buildServer.serverSide.SProjectFeatureDescriptor;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentialsException;
import jetbrains.buildServer.serverSide.impl.BaseServerTestCase;
import jetbrains.buildServer.testUtils.TestUtils;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import software.amazon.awssdk.services.sts.model.Credentials;

@Test
public class AwsCredentialsHolderCacheTest extends BaseServerTestCase {

  private AwsCredentialsHolderCache cache;
  private SProjectFeatureDescriptor myFeatureDescriptor;
  private Credentials myCredentials;
  private AtomicInteger myCounter;
  private RequestSessionFunction myMockSupplier;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    cache = new AwsCredentialsHolderCache(getEventDispatcher(), myProjectManager);
    myCredentials = Mockito.mock(Credentials.class);
    Mockito.when(myCredentials.expiration()).thenReturn(Instant.now().plus(1, ChronoUnit.HOURS));
    myFeatureDescriptor = TestUtils.createConnectionDescriptor(myProject.getProjectId(), "connectionId", Collections.emptyMap());
    myCounter = new AtomicInteger(0);
    myMockSupplier = () -> {
      myCounter.incrementAndGet();
      return myCredentials;
    };
  }

  public void testCachedValueIsReturned() throws ConnectionCredentialsException {
    // Will call twice to make sure cached value is returned
    cache.getAwsCredentials(myFeatureDescriptor, myMockSupplier);
    cache.getAwsCredentials(myFeatureDescriptor, myMockSupplier);

    Assert.assertEquals(myCounter.get(), 1);
  }

  public void testCachedValueIsNotReturned_IfCacheDisabled() throws ConnectionCredentialsException {
    setInternalProperty(AwsCredentialsHolderCache.ENABLE_AWS_CREDENTIALS_CACHE, "false");
    // Will call twice to make sure cached value is not returned
    cache.getAwsCredentials(myFeatureDescriptor, myMockSupplier);
    cache.getAwsCredentials(myFeatureDescriptor, myMockSupplier);

    Assert.assertEquals(myCounter.get(), 2);
  }


  public void testCachedValueIsNotReturned_IfBufferExpires() throws ConnectionCredentialsException {
    setInternalProperty(AwsCredentialsHolderCache.CREDENTIALS_CACHE_EXPIRATION_BUFFER_SECONDS, 2*60*60); //2 hours
    // Will call twice to make sure cached value is not returned
    cache.getAwsCredentials(myFeatureDescriptor, myMockSupplier);
    cache.getAwsCredentials(myFeatureDescriptor, myMockSupplier);

    Assert.assertEquals(myCounter.get(), 2);
  }

  public void testCachedValueIsNotReturned_IfProjectFeatureChanged() throws ConnectionCredentialsException {
    // Will call twice to make sure cached value is not returned
    cache.getAwsCredentials(myFeatureDescriptor, myMockSupplier);
    getEventDispatcher().getMulticaster().projectFeatureChanged(myProject, myFeatureDescriptor, myFeatureDescriptor);
    cache.getAwsCredentials(myFeatureDescriptor, myMockSupplier);

    Assert.assertEquals(myCounter.get(), 2);
  }

  public void testCachedValueIsNotReturned_IfProjectFeatureRemoved() throws ConnectionCredentialsException {
    // Will call twice to make sure cached value is not returned
    cache.getAwsCredentials(myFeatureDescriptor, myMockSupplier);
    getEventDispatcher().getMulticaster().projectFeatureRemoved(myProject, myFeatureDescriptor);
    cache.getAwsCredentials(myFeatureDescriptor, myMockSupplier);

    Assert.assertEquals(myCounter.get(), 2);
  }

  public void testCachedValueIsNotReturned_IfProjectRestored() throws ConnectionCredentialsException {
    // Will call twice to make sure cached value is not returned
    cache.getAwsCredentials(myFeatureDescriptor, myMockSupplier);
    getEventDispatcher().getMulticaster().projectRestored(myProject.getProjectId());
    cache.getAwsCredentials(myFeatureDescriptor, myMockSupplier);

    Assert.assertEquals(myCounter.get(), 2);
  }
}
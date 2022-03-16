package jetbrains.buildServer.clouds.amazon.connector.impl;

import com.amazonaws.auth.BasicSessionCredentials;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.executors.ExecutorServices;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertNotEquals;

public class AwsConnectionCredentialsProviderTest extends BaseTestCase {

  private ExecutorServices myExecutorServices;

  @BeforeMethod
  public void setUp() {
    myExecutorServices = new ExecutorServices() {
      @NotNull
      @Override
      public ScheduledExecutorService getNormalExecutorService() {
        return Executors.newScheduledThreadPool(1);
      }

      @NotNull
      @Override
      public ExecutorService getLowPriorityExecutorService() {
        return null;
      }
    };
  }

  @Test
  public void givenRefreshingAwsCredentials_whenTheyExpire_thenRefreshCredentials() throws InterruptedException {
    AwsConnectionCredentialsProviderForTests test = new AwsConnectionCredentialsProviderForTests(myExecutorServices, 1000, 1500);

    BasicSessionCredentials creds = (BasicSessionCredentials)test.getCredentials();
    assertNotNull(creds.getAWSAccessKeyId());

    Loggers.CLOUD.warn("Waiting for the credentials to expire");
    Thread.sleep(2000);

    BasicSessionCredentials refreshedCreds = (BasicSessionCredentials)test.getCredentials();
    assertNotNull(refreshedCreds.getAWSAccessKeyId());

    assertNotEquals(creds.getSessionToken(), refreshedCreds.getSessionToken());


    creds = (BasicSessionCredentials)test.getCredentials();
    assertEquals(creds.getSessionToken(), refreshedCreds.getSessionToken());
  }
}
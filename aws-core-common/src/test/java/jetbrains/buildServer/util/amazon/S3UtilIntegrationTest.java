

package jetbrains.buildServer.util.amazon;


import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.TestNGUtil;
import jetbrains.buildServer.util.CollectionsUtil;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.DownloadRequest;
import software.amazon.awssdk.transfer.s3.model.Transfer;
import software.amazon.awssdk.transfer.s3.model.UploadRequest;

import static jetbrains.buildServer.util.amazon.AWSCommonParams.*;

/**
 * @author vbedrosova
 */

@Test
public class S3UtilIntegrationTest extends BaseTestCase {
  private static final String BUCKET_NAME = "amazon.util.s3.util.test";
  private S3AsyncClient asyncS3Client;
  private S3Client myS3Client;

  @Override
  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    final Map<String, String> params = getParameters();
    if (StringUtil.isEmptyOrSpaces(params.get(ACCESS_KEY_ID_PARAM)) || StringUtil.isEmptyOrSpaces(params.get(SECURE_SECRET_ACCESS_KEY_PARAM))) {
      TestNGUtil.skip("No credentials specified for tests");
    }
    createS3Clients(params);


    if (bucketExists()) {
      deleteBucket();
    }

    myS3Client.createBucket(req -> req.bucket(BUCKET_NAME));
  }

  private boolean bucketExists() {
    try {
      myS3Client.getBucketAcl(req -> req.bucket(BUCKET_NAME));
      return true;
    } catch (AwsServiceException ex) {
      if ((ex.statusCode() == HttpStatusCode.MOVED_PERMANENTLY) || "AccessDenied".equals(ex.awsErrorDetails().errorCode())) {
        return true;
      }

      if (ex.statusCode() == HttpStatusCode.NOT_FOUND) {
        return false;
      }

      throw ex;
    }
  }

  @Override
  @AfterMethod
  public void tearDown() throws Exception {
    super.tearDown();
    if (myS3Client != null) {
      myS3Client.close();
    }

    if (asyncS3Client != null) {
      asyncS3Client.close();
    }

    createS3Clients(getParameters());
    deleteBucket();
  }

  @Test
  public void shutdown_manager() throws Throwable {
    S3Util.withTransferManager(asyncS3Client, manager -> {
      manager.close();
      return Collections.emptyList();
    });
  }

  @Test
  public void upload() throws Throwable {
    final File testUpload = createTempFile("This is a test upload");
    S3Util.withTransferManager(asyncS3Client, manager -> Collections.singletonList(manager.upload(
      UploadRequest.builder()
        .putObjectRequest(
          PutObjectRequest.builder()
            .bucket(BUCKET_NAME)
            .key("testUploadInterrupt")
            .build()
        )
        .requestBody(AsyncRequestBody.fromFile(testUpload))
        .build())));
  }

  @Test
  public void upload_and_interrupt() throws Throwable {
    final File testUpload = createTempFile(104857600);
    final AtomicReference<S3Util.TransferManagerInterruptHook> interruptHook = new AtomicReference<>();
    final Thread thread = new Thread(() -> {
      try {
        synchronized (interruptHook) {
          interruptHook.wait();
        }
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      try {
        interruptHook.get().interrupt();
      } catch (Throwable t) {
        throw new RuntimeException(t);
      }
    });
    thread.start();
    S3Util.withTransferManager(asyncS3Client, new S3Util.InterruptAwareWithTransferManager<Transfer>() {
      @NotNull
      @Override
      public Collection<Transfer> run(@NotNull S3TransferManager manager) {
        return Collections.singletonList(
          manager.upload(UploadRequest.builder()
            .putObjectRequest(
              PutObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key("testUploadInterrupt")
                .build()
            )
            .requestBody(AsyncRequestBody.fromFile(testUpload))
            .build()));
      }

      @Override
      public void setInterruptHook(@NotNull S3Util.TransferManagerInterruptHook hook) {
        interruptHook.set(hook);
        synchronized (interruptHook) {
          interruptHook.notify();
        }
      }
    });
    try {
      myS3Client.getObject(req -> req.bucket(BUCKET_NAME).key("testUploadInterrupt"));
    } catch (SdkException e) {
      assertContains(e.getMessage(), "The specified key does not exist");
    }
  }

  @Test
  public void download() throws Throwable {
    final File testDownload = createTempFile("This is a test download");
    myS3Client.putObject(
      PutObjectRequest.builder()
        .bucket(BUCKET_NAME)
        .key("testDownload")
      .build(), RequestBody.fromFile(testDownload));
    assertEquals(testDownload.length(),
      myS3Client.getObject(
          GetObjectRequest.builder()
            .bucket(BUCKET_NAME)
            .key("testDownload")
            .build()
        )
        .response()
        .contentLength()
        .longValue());

    final File result = new File(createTempDir(), "testDownload");
    S3Util.withTransferManager(asyncS3Client, manager -> Collections.singletonList(manager.download(DownloadRequest.builder().getObjectRequest(GetObjectRequest.builder().bucket(BUCKET_NAME).key("testDownload").build()).responseTransformer(AsyncResponseTransformer.toFile(result)).build())));
    assertEquals(testDownload.length(), result.length());
  }

  private void deleteBucket() {
    ListObjectsV2Request req = ListObjectsV2Request.builder().bucket(BUCKET_NAME).build();
    myS3Client.listObjectsV2Paginator(req)
      .stream()
      .flatMap(listObjectsV2 -> listObjectsV2.contents().stream())
      .forEach(obj -> myS3Client.deleteObject(rq -> rq.bucket(BUCKET_NAME).key(obj.key())));

    myS3Client.deleteBucket(rq -> rq.bucket(BUCKET_NAME));
  }

  private void createS3Clients(@NotNull final Map<String, String> params) {
    final AWSClients awsClients = AWSClients.fromBasicCredentials(
      params.get(ACCESS_KEY_ID_PARAM),
      params.get(SECURE_SECRET_ACCESS_KEY_PARAM),
      params.get(REGION_NAME_PARAM)
    );
    if (params.get(SERVICE_ENDPOINT_PARAM) != null) {
      awsClients.setServiceEndpoint(params.get(SERVICE_ENDPOINT_PARAM));
    }
    myS3Client = awsClients.createS3Client();
    asyncS3Client = awsClients.createS3AsyncClient(null, null);
  }

  @NotNull
  private Map<String, String> getParameters() {
    return CollectionsUtil.asMap(REGION_NAME_PARAM, Region.US_EAST_1.id(),
      CREDENTIALS_TYPE_PARAM, ACCESS_KEYS_OPTION,
      ACCESS_KEY_ID_PARAM, System.getProperty(ACCESS_KEY_ID_PARAM),
      SECURE_SECRET_ACCESS_KEY_PARAM, System.getProperty(SECURE_SECRET_ACCESS_KEY_PARAM),
      SERVICE_ENDPOINT_PARAM, System.getProperty(SERVICE_ENDPOINT_PARAM));
  }
}
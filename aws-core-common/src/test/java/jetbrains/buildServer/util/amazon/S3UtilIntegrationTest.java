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

package jetbrains.buildServer.util.amazon;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
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

import static jetbrains.buildServer.util.amazon.AWSCommonParams.*;

/**
 * @author vbedrosova
 */

@Test
public class S3UtilIntegrationTest extends BaseTestCase {
  private static final String BUCKET_NAME = "amazon.util.s3.util.test";
  private AmazonS3 myS3Client;

  @Override
  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    final Map<String, String> params = getParameters();
    if (StringUtil.isEmptyOrSpaces(params.get(ACCESS_KEY_ID_PARAM)) || StringUtil.isEmptyOrSpaces(params.get(SECURE_SECRET_ACCESS_KEY_PARAM))) {
      TestNGUtil.skip("No credentials specified for tests");
    }
    createS3Client(params);
    if (myS3Client.doesBucketExistV2(BUCKET_NAME)) {
      deleteBucket();
    }
    myS3Client.createBucket(BUCKET_NAME);
  }

  @Override
  @AfterMethod
  public void tearDown() throws Exception {
    super.tearDown();
    if (myS3Client != null) {
      myS3Client.shutdown();
    }
    createS3Client(getParameters());
    deleteBucket();
  }

  @Test
  public void shutdown_manager() throws Throwable {
    S3Util.withTransferManager(myS3Client, manager -> {
      manager.shutdownNow();
      return Collections.emptyList();
    });
  }

  @Test
  public void upload() throws Throwable {
    final File testUpload = createTempFile("This is a test upload");
    S3Util.withTransferManager(myS3Client, manager -> Collections.singletonList(manager.upload(BUCKET_NAME, "testUpload", testUpload)));
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
    S3Util.withTransferManager(myS3Client, new S3Util.InterruptAwareWithTransferManager<Upload>() {
      @NotNull
      @Override
      public Collection<Upload> run(@NotNull TransferManager manager) {
        return Collections.singletonList(manager.upload(BUCKET_NAME, "testUploadInterrupt", testUpload));
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
      myS3Client.getObject(BUCKET_NAME, "testUploadInterrupt");
    } catch (AmazonS3Exception e) {
      assertContains(e.getMessage(), "The specified key does not exist");
    }
  }

  @Test
  public void download() throws Throwable {
    final File testDownload = createTempFile("This is a test download");
    myS3Client.putObject(BUCKET_NAME, "testDownload", testDownload);
    assertEquals(testDownload.length(), myS3Client.getObject(BUCKET_NAME, "testDownload").getObjectMetadata().getContentLength());

    final File result = new File(createTempDir(), "testDownload");
    S3Util.withTransferManager(myS3Client, manager -> Collections.singletonList(manager.download(BUCKET_NAME, "testDownload", result)));
    assertEquals(testDownload.length(), result.length());
  }

  private void deleteBucket() {
    ObjectListing listing = myS3Client.listObjects(BUCKET_NAME);
    while (true) {
      for (S3ObjectSummary s3ObjectSummary : listing.getObjectSummaries()) {
        myS3Client.deleteObject(BUCKET_NAME, s3ObjectSummary.getKey());
      }
      if (listing.isTruncated()) {
        listing = myS3Client.listNextBatchOfObjects(listing);
      } else {
        break;
      }
    }
    myS3Client.deleteBucket(S3UtilIntegrationTest.BUCKET_NAME);
  }

  private void createS3Client(@NotNull final Map<String, String> params) {
    final AWSClients awsClients = AWSClients.fromBasicCredentials(
      params.get(ACCESS_KEY_ID_PARAM),
      params.get(SECURE_SECRET_ACCESS_KEY_PARAM),
      params.get(REGION_NAME_PARAM)
    );
    if (params.get(SERVICE_ENDPOINT_PARAM) != null) {
      awsClients.setServiceEndpoint(params.get(SERVICE_ENDPOINT_PARAM));
    }
    myS3Client = awsClients.createS3Client();
  }

  @NotNull
  private Map<String, String> getParameters() {
    return CollectionsUtil.asMap(REGION_NAME_PARAM, Regions.DEFAULT_REGION.getName(),
                                 CREDENTIALS_TYPE_PARAM, ACCESS_KEYS_OPTION,
                                 ACCESS_KEY_ID_PARAM, System.getProperty(ACCESS_KEY_ID_PARAM),
                                 SECURE_SECRET_ACCESS_KEY_PARAM, System.getProperty(SECURE_SECRET_ACCESS_KEY_PARAM),
                                 SERVICE_ENDPOINT_PARAM, System.getProperty(SERVICE_ENDPOINT_PARAM));
  }
}
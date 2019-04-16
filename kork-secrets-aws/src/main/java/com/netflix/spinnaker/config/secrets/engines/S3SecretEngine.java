/*
 * Copyright 2019 Armory, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.config.secrets.engines;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.S3Object;
import com.netflix.spinnaker.config.secrets.EncryptedSecret;
import com.netflix.spinnaker.config.secrets.SecretException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
public class S3SecretEngine extends AbstractStorageSecretEngine {
  private static String IDENTIFIER = "s3";

  public String identifier() { return S3SecretEngine.IDENTIFIER;}


  @Override
  protected InputStream downloadRemoteFile(EncryptedSecret encryptedSecret) throws IOException {
    String region = encryptedSecret.getParams().get(STORAGE_REGION);
    String bucket = encryptedSecret.getParams().get(STORAGE_BUCKET);
    String objName = encryptedSecret.getParams().get(STORAGE_FILE_URI);

    AmazonS3ClientBuilder s3ClientBuilder = AmazonS3ClientBuilder.standard()
      .withRegion(region);

    AmazonS3 s3Client = s3ClientBuilder.build();

    try {
      if (!s3Client.doesBucketExistV2(bucket)) {
        throw new SecretException(String.format("S3 Bucket does not exist. Bucket: %s, Region: %s", bucket, region));
      }

      S3Object s3Object = s3Client.getObject(bucket, objName);

      return s3Object.getObjectContent();
    } catch (AmazonS3Exception ex) {
      StringBuilder sb = new StringBuilder("Error reading contents of S3 -- ");
      if (403 == ex.getStatusCode()) {
        sb.append(String.format(
          "Unauthorized access. Check connectivity and permissions to the bucket. -- Bucket: %s, Object: %s, Region: %s.\n" +
            "Error: %s ", bucket, objName, region, ex.toString()));
      } else if (404 == ex.getStatusCode()) {
        sb.append(String.format(
          "Not found. Does secret file exist? -- Bucket: %s, Object: %s, Region: %s.\nError: %s",
          bucket, objName, region, ex.toString()));
      } else {
        sb.append(String.format("Error: %s", ex.toString()));
      }
      throw new SecretException(sb.toString());
    } catch (AmazonClientException ex) {
      throw new SecretException(String.format(
        "Error reading contents of S3. Bucket: %s, Object: %s, Region: %s.\nError: %s",
        bucket, objName, region, ex.toString()));
    }
  }
}

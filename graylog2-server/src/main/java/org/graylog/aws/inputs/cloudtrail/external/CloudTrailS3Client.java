package org.graylog.aws.inputs.cloudtrail.external;

import org.graylog2.plugin.InputFailureRecorder;
import org.graylog2.plugin.Tools;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URI;

import static org.graylog2.shared.utilities.StringUtils.f;

public class CloudTrailS3Client {

    private final InputFailureRecorder inputFailureRecorder;
    private final S3Client s3Client;

    public CloudTrailS3Client(@Nullable URI endpointOverride, String awsRegion, AwsCredentialsProvider credentialsProvider,
                              InputFailureRecorder inputFailureRecorder) {
        this.inputFailureRecorder = inputFailureRecorder;

        final S3ClientBuilder clientBuilder = S3Client.builder()
                .httpClientBuilder(ApacheHttpClient.builder())
                .region(Region.of(awsRegion))
                .credentialsProvider(credentialsProvider);

        if (endpointOverride != null) {
            clientBuilder.endpointOverride(endpointOverride).forcePathStyle(true);
        }

        s3Client = clientBuilder.build();
    }

    public String readCompressed(String bucket, String key) throws IOException {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        try {
            ResponseBytes<?> objectBytes = this.s3Client.getObjectAsBytes(getObjectRequest);

            if (objectBytes == null) {
                String errorMessage = "Received null response when fetching S3 object from bucket [" + bucket + "], key [" + key + "].";
                inputFailureRecorder.setFailing(getClass(), errorMessage);
                throw new RuntimeException(errorMessage);
            }

            byte[] bytes = objectBytes.asByteArray();
            return Tools.decompressGzip(bytes);

        } catch (Exception e) {
            String errorMessage = f("Failed to read or decompress S3 object from bucket [%s], key [%s]: %s", bucket, key, e.getMessage());
            inputFailureRecorder.setFailing(getClass(), errorMessage);
            throw new IOException(errorMessage, e);
        }
    }


}

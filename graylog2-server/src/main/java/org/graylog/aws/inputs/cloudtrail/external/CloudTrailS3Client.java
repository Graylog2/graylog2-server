/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.aws.inputs.cloudtrail.external;

import com.google.common.base.Splitter;
import jakarta.annotation.Nullable;
import org.graylog2.plugin.InputFailureRecorder;
import org.graylog2.plugin.Tools;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.apache.ProxyConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.graylog2.shared.utilities.StringUtils.f;

public class CloudTrailS3Client {

    private final InputFailureRecorder inputFailureRecorder;
    private final S3Client s3Client;

    public CloudTrailS3Client(@Nullable URI endpointOverride, String awsRegion, AwsCredentialsProvider credentialsProvider,
                              InputFailureRecorder inputFailureRecorder, @Nullable URI proxyUri) {
        this.inputFailureRecorder = inputFailureRecorder;

        final ApacheHttpClient.Builder httpClientBuilder = ApacheHttpClient.builder();
        if (proxyUri != null) {
            httpClientBuilder.proxyConfiguration(buildProxyConfiguration(proxyUri));
        }

        final S3ClientBuilder clientBuilder = S3Client.builder()
                .httpClientBuilder(httpClientBuilder)
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

    /**
     * Builds a ProxyConfiguration from a proxy URI, extracting and setting credentials if present.
     * <p>
     * AWS SDK v2 does not support user credentials in the proxy endpoint URI. If the URI contains
     * user info (username:password@host:port), this method strips it from the endpoint and sets
     * the credentials separately on the ProxyConfiguration.Builder.
     * </p>
     *
     * @param proxyUri the proxy URI, potentially containing user credentials
     * @return a configured ProxyConfiguration
     */
    public static ProxyConfiguration buildProxyConfiguration(URI proxyUri) {
        ProxyConfiguration.Builder proxyConfigBuilder = ProxyConfiguration.builder();

        // Check if proxy URI contains user credentials
        if (!isNullOrEmpty(proxyUri.getUserInfo())) {
            // Extract username and password from user info
            final List<String> credentials = Splitter.on(":")
                    .limit(2)
                    .splitToList(proxyUri.getUserInfo());

            if (credentials.size() == 2) {
                proxyConfigBuilder.username(credentials.get(0));
                proxyConfigBuilder.password(credentials.get(1));
            }

            // Create a clean URI without user info for the endpoint
            try {
                URI cleanProxyUri = new URI(
                        proxyUri.getScheme(),
                        null, // userInfo - set to null to strip credentials
                        proxyUri.getHost(),
                        proxyUri.getPort(),
                        proxyUri.getPath(),
                        proxyUri.getQuery(),
                        proxyUri.getFragment()
                );
                proxyConfigBuilder.endpoint(cleanProxyUri);
            } catch (URISyntaxException e) {
                // If we can't create a clean URI, fall back to the original
                // This will likely fail with the AWS SDK validation error, but preserves existing behavior
                proxyConfigBuilder.endpoint(proxyUri);
            }
        } else {
            // No credentials in URI, use as-is
            proxyConfigBuilder.endpoint(proxyUri);
        }

        return proxyConfigBuilder.build();
    }


}

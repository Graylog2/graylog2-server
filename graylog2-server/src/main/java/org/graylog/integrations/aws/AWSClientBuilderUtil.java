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
package org.graylog.integrations.aws;

import com.google.common.base.Preconditions;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.ws.rs.BadRequestException;
import org.apache.commons.lang3.StringUtils;
import org.graylog.aws.AWSAsyncProxyConfigurationProvider;
import org.graylog.aws.AWSProxyConfigurationProvider;
import org.graylog.integrations.aws.resources.requests.AWSRequest;
import org.graylog2.Configuration;
import org.graylog2.security.encryption.EncryptedValue;
import org.graylog2.security.encryption.EncryptedValueService;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsSyncClientBuilder;
import software.amazon.awssdk.core.client.builder.SdkClientBuilder;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.nio.netty.Http2Configuration;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClientBuilder;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.IamClientBuilder;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.KinesisClientBuilder;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;

/**
 * Responsible for initializing and building AWS SDK clients. This logic is centralized in one place to ensure consistency amongst the
 * clients and their initialization.
 */
public class AWSClientBuilderUtil {

    // HTTP/2 tuning required by the Kinesis Client Library. These mirror the values applied by
    // software.amazon.kinesis.common.KinesisClientUtil#adjustKinesisClientBuilder, which we cannot use directly
    // because it would override our proxy-aware HTTP client builder.
    private static final int KINESIS_HTTP2_INITIAL_WINDOW_SIZE_BYTES = 512 * 1024; // 512 KB
    private static final long KINESIS_HTTP2_HEALTH_CHECK_PING_PERIOD_MILLIS = 60 * 1000;

    private final Provider<AWSAuthFactory> authFactoryProvider;
    private final EncryptedValueService encryptedValueService;
    private final Configuration configuration;
    private final AWSProxyConfigurationProvider proxyConfigurationProvider;
    private final AWSAsyncProxyConfigurationProvider asyncProxyConfigurationProvider;

    @Inject
    public AWSClientBuilderUtil(Provider<AWSAuthFactory> authFactoryProvider, EncryptedValueService encryptedValueService,
                                Configuration configuration, AWSProxyConfigurationProvider proxyConfigurationProvider,
                                AWSAsyncProxyConfigurationProvider asyncProxyConfigurationProvider) {
        this.authFactoryProvider = authFactoryProvider;
        this.encryptedValueService = encryptedValueService;
        this.configuration = configuration;
        this.proxyConfigurationProvider = proxyConfigurationProvider;
        this.asyncProxyConfigurationProvider = asyncProxyConfigurationProvider;
    }

    public AwsCredentialsProvider createCredentialsProvider(AWSRequest request) {
        if (StringUtils.isNotBlank(request.externalId()) && StringUtils.isBlank(request.assumeRoleArn())) {
            throw new BadRequestException("External ID can only be used when an Assume Role ARN is provided.");
        }
        return authFactoryProvider.get().create(
                configuration.isCloud(),
                request.region(),
                request.awsAccessKeyId(),
                decryptSecretAccessKey(request.awsSecretAccessKey()),
                request.assumeRoleArn(),
                request.externalId());
    }

    /**
     * Creates an AWS credentials provider with proxy support on the STS client used for assume-role.
     */
    public AwsCredentialsProvider createCredentialsProviderWithStsProxy(AWSRequest request) {
        if (StringUtils.isNotBlank(request.externalId()) && StringUtils.isBlank(request.assumeRoleArn())) {
            throw new BadRequestException("External ID can only be used when an Assume Role ARN is provided.");
        }
        return authFactoryProvider.get().create(
                configuration.isCloud(),
                request.region(),
                request.awsAccessKeyId(),
                decryptSecretAccessKey(request.awsSecretAccessKey()),
                request.assumeRoleArn(),
                request.externalId(),
                proxyConfigurationProvider.get());
    }

    /**
     * Initialize the builder with the appropriate authorization, region, and endpoints.
     *
     * @param builder  Any AWS client builder.
     * @param endpoint See {@link SdkClientBuilder#endpointOverride(java.net.URI)} javadoc.
     * @param region   The region to specify on the client.
     */
    public void initializeBuilder(AwsClientBuilder builder, String endpoint, Region region, AwsCredentialsProvider credentialsProvider) {
        builder.region(region);
        builder.credentialsProvider(credentialsProvider);

        // The endpoint override explicitly overrides the default URL used for all AWS API communication.
        if (StringUtils.isNotEmpty(endpoint)) {
            builder.endpointOverride(URI.create(endpoint));
        }
    }

    /**
     * Initialize and build the CloudWatch client.
     *
     * @param clientBuilder The builder, which was supplied through dependency injection.
     * @param request       The full AWSRequest.
     * @return A fully built {@link CloudWatchLogsClient}
     */
    public CloudWatchLogsClient buildClient(CloudWatchLogsClientBuilder clientBuilder, AWSRequest request) {
        Preconditions.checkNotNull(request.region(), "An AWS region is required.");
        initializeBuilder(clientBuilder,
                request.cloudwatchEndpoint(),
                Region.of(request.region()),
                createCredentialsProviderWithStsProxy(request));
        applySyncProxy(clientBuilder);

        return clientBuilder.build();
    }

    /**
     * Initialize and build the Kinesis client.
     *
     * @param clientBuilder The builder, which was supplied through dependency injection.
     * @param request       The full AWSRequest.
     * @return A fully built {@link KinesisClient}
     */
    public KinesisClient buildClient(KinesisClientBuilder clientBuilder, AWSRequest request) {
        initializeBuilder(clientBuilder,
                request.kinesisEndpoint(),
                Region.of(request.region()),
                createCredentialsProviderWithStsProxy(request));
        applySyncProxy(clientBuilder);

        return clientBuilder.build();
    }

    /**
     * Initialize and build the IAM client.
     *
     * @param clientBuilder The builder, which was supplied through dependency injection.
     * @param request       The full AWSRequest.
     * @return A fully built {@link IamClient}
     */
    public IamClient buildClient(IamClientBuilder clientBuilder, AWSRequest request) {
        Region iamRegion = Region.AWS_GLOBAL;
        if (request.region().contains("gov")) {
            iamRegion = Region.AWS_US_GOV_GLOBAL;
        }

        initializeBuilder(clientBuilder,
                request.iamEndpoint(),
                iamRegion, // Always specify the appropriate global region for the IAM client.
                createCredentialsProviderWithStsProxy(request));
        applySyncProxy(clientBuilder);
        return clientBuilder.build();
    }

    /**
     * Applies the proxy-aware Apache HTTP client builder to a synchronous AWS client builder. This is a no-op when
     * no {@code http_proxy_uri} is configured.
     */
    private void applySyncProxy(AwsSyncClientBuilder<?, ?> clientBuilder) {
        clientBuilder.httpClientBuilder(proxyConfigurationProvider.get());
    }

    /**
     * Returns a Netty async HTTP client builder configured with the Graylog HTTP proxy (when set). Use this for the
     * DynamoDB and CloudWatch async clients required by the Kinesis input. These do not require HTTP/2.
     */
    public NettyNioAsyncHttpClient.Builder asyncHttpClientBuilder() {
        return asyncProxyConfigurationProvider.get();
    }

    /**
     * Returns a Netty async HTTP client builder configured for HTTP/2 (required by the Kinesis Client Library) plus the
     * Graylog HTTP proxy (when set).
     * <p>
     * This replaces {@link software.amazon.kinesis.common.KinesisClientUtil#createKinesisAsyncClient}, which always
     * installs its own HTTP client builder and would therefore discard our proxy configuration. The HTTP/2 settings
     * mirror the values applied by that utility.
     */
    public NettyNioAsyncHttpClient.Builder kinesisAsyncHttpClientBuilder() {
        return asyncProxyConfigurationProvider.get()
                .maxConcurrency(Integer.MAX_VALUE)
                .http2Configuration(Http2Configuration.builder()
                        .initialWindowSize(KINESIS_HTTP2_INITIAL_WINDOW_SIZE_BYTES)
                        .healthCheckPingPeriod(Duration.ofMillis(KINESIS_HTTP2_HEALTH_CHECK_PING_PERIOD_MILLIS))
                        .build())
                .protocol(Protocol.HTTP2);
    }

    private String decryptSecretAccessKey(EncryptedValue secretAccessKey) {
        return encryptedValueService.decrypt(Optional.ofNullable(secretAccessKey).orElse(EncryptedValue.createUnset()));
    }
}

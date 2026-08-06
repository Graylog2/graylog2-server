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
import jakarta.ws.rs.BadRequestException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.StsClientBuilder;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.auth.StsCredentialsProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.SdkAutoCloseable;

import javax.annotation.Nullable;
import java.util.Locale;

import static com.google.common.base.Strings.isNullOrEmpty;

public class AWSAuthFactory {
    private static final Logger LOG = LoggerFactory.getLogger(AWSAuthFactory.class);

    /**
     * Resolves the appropriate AWS authorization provider based on the input.
     * If an {@code accessKey} and {@code secretKey} are provided, they will be used explicitly.
     * If not, the default DefaultCredentialsProvider will be used instead. This will resolve the role/policy
     * using Java props, environment variables, EC2 instance roles etc. See the {@link DefaultCredentialsProvider}
     * Javadoc for more information.
     */
    public AwsCredentialsProvider create(boolean requireKeySecret,
                                         @Nullable String stsRegion,
                                         @Nullable String accessKey,
                                         @Nullable String secretKey,
                                         @Nullable String assumeRoleArn) {
        return create(requireKeySecret, stsRegion, accessKey, secretKey, assumeRoleArn, null, null);
    }

    public AwsCredentialsProvider create(boolean requireKeySecret,
                                         @Nullable String stsRegion,
                                         @Nullable String accessKey,
                                         @Nullable String secretKey,
                                         @Nullable String assumeRoleArn,
                                         @Nullable String externalId) {
        return create(requireKeySecret, stsRegion, accessKey, secretKey, assumeRoleArn, externalId, null);
    }

    /**
     * Resolves the appropriate AWS authorization provider, optionally configuring an HTTP client builder
     * on the STS client used for assume-role. This allows callers to pass proxy configuration for
     * the STS client without affecting other callers.
     *
     * @param stsHttpClientBuilder optional Apache HTTP client builder (e.g. with proxy config) for the STS client
     */
    public AwsCredentialsProvider create(boolean requireKeySecret,
                                         @Nullable String stsRegion,
                                         @Nullable String accessKey,
                                         @Nullable String secretKey,
                                         @Nullable String assumeRoleArn,
                                         @Nullable ApacheHttpClient.Builder stsHttpClientBuilder) {
        return create(requireKeySecret, stsRegion, accessKey, secretKey, assumeRoleArn, null, stsHttpClientBuilder);
    }

    /**
     * Resolves the appropriate AWS authorization provider, optionally configuring an External ID
     * on the STS AssumeRole request to prevent the confused deputy problem, and optionally configuring
     * an HTTP client builder on the STS client used for assume-role.
     *
     * @param externalId           optional external ID to include in the AssumeRole request
     * @param stsHttpClientBuilder optional Apache HTTP client builder (e.g. with proxy config) for the STS client
     */
    public AwsCredentialsProvider create(boolean requireKeySecret,
                                         @Nullable String stsRegion,
                                         @Nullable String accessKey,
                                         @Nullable String secretKey,
                                         @Nullable String assumeRoleArn,
                                         @Nullable String externalId,
                                         @Nullable ApacheHttpClient.Builder stsHttpClientBuilder) {
        // Discards the STS client handle. Callers on this overload cannot close the STS client backing an
        // assume-role provider, so its connection pool leaks for the process lifetime; see createCloseable.
        return createCloseable(requireKeySecret, stsRegion, accessKey, secretKey, assumeRoleArn, externalId, stsHttpClientBuilder)
                .provider();
    }

    /**
     * Same resolution as {@link #create(boolean, String, String, String, String, String, ApacheHttpClient.Builder)},
     * but returns a {@link CredentialsProviderHandle} so the caller can close the STS client backing an assume-role
     * provider.
     *
     * <p>For an assume-role configuration the STS client is created here and handed to
     * {@link StsAssumeRoleCredentialsProvider}, which never closes it: {@link StsCredentialsProvider#close()} only
     * clears the session cache. Callers that build a fresh provider per operation must therefore close the handle,
     * or the STS client's Apache connection pool leaks on every call.
     */
    public CredentialsProviderHandle createCloseable(boolean requireKeySecret,
                                                     @Nullable String stsRegion,
                                                     @Nullable String accessKey,
                                                     @Nullable String secretKey,
                                                     @Nullable String assumeRoleArn,
                                                     @Nullable String externalId,
                                                     @Nullable ApacheHttpClient.Builder stsHttpClientBuilder) {
        AwsCredentialsProvider awsCredentials = requireKeySecret ? getKeySecretCredentialsProvider(accessKey, secretKey) :
                getAwsCredentialsProvider(accessKey, secretKey);

        if (StringUtils.isNotBlank(externalId) && StringUtils.isBlank(assumeRoleArn)) {
            throw new BadRequestException("External ID can only be used when an Assume Role ARN is provided.");
        }

        // Apply the Assume Role ARN Authorization if specified. All AWSCredentialsProviders support this.
        if (!isNullOrEmpty(assumeRoleArn) && !isNullOrEmpty(stsRegion)) {
            LOG.debug("Creating cross account assume role credentials");
            return buildStsCredentialsProvider(awsCredentials, stsRegion, assumeRoleArn, accessKey, externalId, stsHttpClientBuilder);
        }

        return new CredentialsProviderHandle(awsCredentials, null);
    }

    private static AwsCredentialsProvider getAwsCredentialsProvider(String accessKey, String secretKey) {
        AwsCredentialsProvider awsCredentials;
        if (!isNullOrEmpty(accessKey) && !isNullOrEmpty(secretKey)) {
            LOG.debug("Using explicitly provided key and secret.");
            awsCredentials = getKeySecretCredentialsProvider(accessKey, secretKey);
        } else {
            LOG.debug("Using default authorization provider chain.");
            awsCredentials = DefaultCredentialsProvider.builder().build();
        }
        return awsCredentials;
    }

    private static AwsCredentialsProvider getKeySecretCredentialsProvider(String accessKey, String secretKey) {
        Preconditions.checkArgument(StringUtils.isNotBlank(accessKey), "Access key is required.");
        Preconditions.checkArgument(StringUtils.isNotBlank(secretKey), "Secret key is required.");
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
    }

    /**
     * Build a new AwsCredentialsProvider instance which will assume the indicated role.
     * Note: In order to assume a role, a role must be provided to the AWS STS client a role that has the "sts:AssumeRole"
     * permission, which provides authorization for a role to be assumed.
     */
    private static CredentialsProviderHandle buildStsCredentialsProvider(AwsCredentialsProvider awsCredentials, String stsRegion,
                                                                         String assumeRoleArn, @Nullable String accessKey,
                                                                         @Nullable String externalId,
                                                                         @Nullable ApacheHttpClient.Builder stsHttpClientBuilder) {

        final StsClientBuilder stsClientBuilder = StsClient.builder()
                .region(Region.of(stsRegion))
                .credentialsProvider(awsCredentials);
        if (stsHttpClientBuilder != null) {
            stsClientBuilder.httpClientBuilder(stsHttpClientBuilder);
        }
        final StsClient stsClient = stsClientBuilder.build();
        // getCallerIdentity() can throw before the handle owns the client; close it here or it leaks.
        try {
            // The custom roleSessionName is extra metadata, which will be logged in AWS CloudTrail with each request
            // to help with auditing and debugging.
            final String roleSessionName;
            if (accessKey != null) {
                roleSessionName = String.format(Locale.ROOT, "ACCESS_KEY_%s@ACCOUNT_%s", accessKey,
                        stsClient.getCallerIdentity(GetCallerIdentityRequest.builder().build()).account());
            } else {
                roleSessionName = String.format(Locale.ROOT, "ACCOUNT_%s",
                        stsClient.getCallerIdentity(GetCallerIdentityRequest.builder().build()).account());
            }

            LOG.debug("Cross account role session name: " + roleSessionName);
            final AssumeRoleRequest.Builder assumeRoleRequestBuilder = AssumeRoleRequest.builder()
                    .roleSessionName(roleSessionName)
                    .roleArn(assumeRoleArn);
            if (!isNullOrEmpty(externalId)) {
                assumeRoleRequestBuilder.externalId(externalId);
            }
            final StsAssumeRoleCredentialsProvider provider = StsAssumeRoleCredentialsProvider.builder()
                    .refreshRequest(assumeRoleRequestBuilder.build())
                    .stsClient(stsClient)
                    .build();
            return new CredentialsProviderHandle(provider, stsClient);
        } catch (RuntimeException e) {
            IoUtils.closeQuietly(stsClient, LOG);
            throw e;
        }
    }

    /**
     * A resolved {@link AwsCredentialsProvider} paired with the STS client backing an assume-role provider, if any.
     *
     * <p>{@link StsCredentialsProvider#close()} only clears the provider's session cache; the {@link StsClient} it
     * was handed is caller-supplied and {@code final}, so it is never closed and its Apache connection pool leaks.
     * This handle owns both resources so a caller can release them together. {@link #close()} closes the credentials
     * provider first -- stopping its background session refresh -- and then the STS client.
     */
    public static final class CredentialsProviderHandle implements AutoCloseable {
        private final AwsCredentialsProvider provider;
        @Nullable
        private final SdkAutoCloseable stsClient;

        CredentialsProviderHandle(AwsCredentialsProvider provider, @Nullable SdkAutoCloseable stsClient) {
            this.provider = provider;
            this.stsClient = stsClient;
        }

        public AwsCredentialsProvider provider() {
            return provider;
        }

        @Override
        public void close() {
            if (provider instanceof SdkAutoCloseable closeableProvider) {
                IoUtils.closeQuietly(closeableProvider, LOG);
            }
            IoUtils.closeQuietly(stsClient, LOG);
        }
    }
}

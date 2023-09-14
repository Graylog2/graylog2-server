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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest;

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
    public static AwsCredentialsProvider create(boolean isCloud,
                                                @Nullable String stsRegion,
                                                @Nullable String accessKey,
                                                @Nullable String secretKey,
                                                @Nullable String assumeRoleArn) {
        AwsCredentialsProvider awsCredentials = isCloud ? getCloudAwsCredentialsProvider(accessKey, secretKey) :
                getAwsCredentialsProvider(accessKey, secretKey);

        // Apply the Assume Role ARN Authorization if specified. All AWSCredentialsProviders support this.
        if (!isNullOrEmpty(assumeRoleArn) && !isNullOrEmpty(stsRegion)) {
            LOG.debug("Creating cross account assume role credentials");
            return buildStsCredentialsProvider(awsCredentials, stsRegion, assumeRoleArn, accessKey);
        }

        return awsCredentials;
    }

    private static AwsCredentialsProvider getAwsCredentialsProvider(String accessKey, String secretKey) {
        AwsCredentialsProvider awsCredentials;
        if (!isNullOrEmpty(accessKey) && !isNullOrEmpty(secretKey)) {
            LOG.debug("Using explicitly provided key and secret.");
            awsCredentials = StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
        } else {
            LOG.debug("Using default authorization provider chain.");
            awsCredentials = DefaultCredentialsProvider.create();
        }
        return awsCredentials;
    }

    private static AwsCredentialsProvider getCloudAwsCredentialsProvider(String accessKey, String secretKey) {
        Preconditions.checkArgument(StringUtils.isNotBlank(accessKey), "Access key is required.");
        Preconditions.checkArgument(StringUtils.isNotBlank(secretKey), "Secret key is required.");
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
    }

    /**
     * Build a new AwsCredentialsProvider instance which will assume the indicated role.
     * Note: In order to assume a role, a role must be provided to the AWS STS client a role that has the "sts:AssumeRole"
     * permission, which provides authorization for a role to be assumed.
     */
    private static AwsCredentialsProvider buildStsCredentialsProvider(AwsCredentialsProvider awsCredentials, String stsRegion,
                                                                      String assumeRoleArn, @Nullable String accessKey) {

        StsClient stsClient = StsClient.builder().region(Region.of(stsRegion)).credentialsProvider(awsCredentials).build();

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
        return StsAssumeRoleCredentialsProvider.builder().refreshRequest(AssumeRoleRequest.builder()
                        .roleSessionName(roleSessionName)
                        .roleArn(assumeRoleArn)
                        .build())
                .stsClient(stsClient)
                .build();
    }
}

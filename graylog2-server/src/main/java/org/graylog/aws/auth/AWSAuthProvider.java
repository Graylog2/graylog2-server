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
package org.graylog.aws.auth;


import org.graylog.aws.config.AWSPluginConfiguration;
import org.graylog.integrations.aws.AWSAuthFactory;
import org.graylog2.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

import javax.annotation.Nullable;

import static com.google.common.base.Strings.isNullOrEmpty;

public class AWSAuthProvider implements AwsCredentialsProvider {
    private final AwsCredentialsProvider credentials;

    public AWSAuthProvider(Configuration configuration, AWSPluginConfiguration awsConfig) {
        this(configuration, awsConfig, null, null, null, null);
    }

    public AWSAuthProvider(Configuration configuration,
                           AWSPluginConfiguration awsConfig,
                           @Nullable String accessKey,
                           @Nullable String secretKey,
                           @Nullable String region,
                           @Nullable String assumeRoleArn) {
        this.credentials = new AWSAuthFactory().create(
                configuration.isCloud(),
                region,
                resolveAccessKey(configuration, awsConfig, accessKey, secretKey),
                resolveSecretKey(configuration, awsConfig, accessKey, secretKey),
                assumeRoleArn);
    }

    // The cloud codepath requires an explicit access/secret key (validated by AWSAuthFactory) and never falls back
    // to the AWS Plugin configuration, matching the original v1 behavior.
    private static String resolveAccessKey(Configuration configuration, AWSPluginConfiguration awsConfig,
                                           @Nullable String accessKey, @Nullable String secretKey) {
        if (!configuration.isCloud() && (isNullOrEmpty(accessKey) || isNullOrEmpty(secretKey)) && hasPluginConfigCredentials(configuration, awsConfig)) {
            return awsConfig.accessKey();
        }
        return accessKey;
    }

    private static String resolveSecretKey(Configuration configuration, AWSPluginConfiguration awsConfig,
                                           @Nullable String accessKey, @Nullable String secretKey) {
        if (!configuration.isCloud() && (isNullOrEmpty(accessKey) || isNullOrEmpty(secretKey)) && hasPluginConfigCredentials(configuration, awsConfig)) {
            return awsConfig.secretKey(configuration.getPasswordSecret());
        }
        return secretKey;
    }

    private static boolean hasPluginConfigCredentials(Configuration configuration, AWSPluginConfiguration awsConfig) {
        return !isNullOrEmpty(awsConfig.accessKey()) && !isNullOrEmpty(awsConfig.secretKey(configuration.getPasswordSecret()));
    }

    @Override
    public AwsCredentials resolveCredentials() {
        return credentials.resolveCredentials();
    }
}

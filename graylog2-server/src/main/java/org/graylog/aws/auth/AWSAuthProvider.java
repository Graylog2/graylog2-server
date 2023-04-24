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


import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.graylog.aws.config.AWSPluginConfiguration;
import org.graylog2.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

import static com.google.common.base.Strings.isNullOrEmpty;

public class AWSAuthProvider implements AWSCredentialsProvider {
    private static final Logger LOG = LoggerFactory.getLogger(AWSAuthProvider.class);
    private final Configuration configuration;

    private final AWSCredentialsProvider credentials;

    public AWSAuthProvider(Configuration configuration, AWSPluginConfiguration awsConfig) {
        this(configuration, awsConfig, null, null, null, null);
    }

    public AWSAuthProvider(Configuration configuration,
                           AWSPluginConfiguration awsConfig,
                           @Nullable String accessKey,
                           @Nullable String secretKey,
                           @Nullable String region,
                           @Nullable String assumeRoleArn) {
        this.configuration = configuration;
        this.credentials = this.resolveAuthentication(awsConfig, accessKey, secretKey, region, assumeRoleArn);
    }

    private AWSCredentialsProvider resolveAuthentication(AWSPluginConfiguration config,
                                                         @Nullable String accessKey,
                                                         @Nullable String secretKey,
                                                         @Nullable String region,
                                                         @Nullable String assumeRoleArn) {

        AWSCredentialsProvider awsCredentials = configuration.isCloud() ?
                getCloudAwsCredentialsProvider(accessKey, secretKey) :
                getStdAwsCredentialsProvider(config, accessKey, secretKey);

        if (!isNullOrEmpty(assumeRoleArn) && !isNullOrEmpty(region)) {
            LOG.debug("Creating cross account assume role credentials");
            return this.getSTSCredentialsProvider(awsCredentials, region, assumeRoleArn);
        } else {
            return awsCredentials;
        }
    }

    private AWSCredentialsProvider getStdAwsCredentialsProvider(AWSPluginConfiguration config, String accessKey, String secretKey) {
        AWSCredentialsProvider awsCredentials;
        if (!isNullOrEmpty(accessKey) && !isNullOrEmpty(secretKey)) {
            awsCredentials = new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey));
            LOG.debug("Using input specific config");
        } else if (!isNullOrEmpty(config.accessKey()) && !isNullOrEmpty(config.secretKey(configuration.getPasswordSecret()))) {
            awsCredentials = new AWSStaticCredentialsProvider(new BasicAWSCredentials(config.accessKey(), config.secretKey(configuration.getPasswordSecret())));
            LOG.debug("Using AWS Plugin config");
        } else {
            awsCredentials = new DefaultAWSCredentialsProviderChain();
            LOG.debug("Using Default Provider Chain");
        }
        return awsCredentials;
    }

    private AWSCredentialsProvider getCloudAwsCredentialsProvider(String accessKey, String secretKey) {
        Preconditions.checkArgument(StringUtils.isNotBlank(accessKey), "Access key is required.");
        Preconditions.checkArgument(StringUtils.isNotBlank(secretKey), "Secret key is required.");
        return new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey));
    }

    private AWSCredentialsProvider getSTSCredentialsProvider(AWSCredentialsProvider awsCredentials, String region, String assumeRoleArn) {
        AWSSecurityTokenService stsClient = AWSSecurityTokenServiceClientBuilder.standard()
                .withRegion(region)
                .withCredentials(awsCredentials)
                .build();
        String roleSessionName = String.format("API_KEY_%s@ACCOUNT_%s",
                awsCredentials.getCredentials().getAWSAccessKeyId(),
                stsClient.getCallerIdentity(new GetCallerIdentityRequest()).getAccount());
        LOG.debug("Cross account role session name: " + roleSessionName);
        return new STSAssumeRoleSessionCredentialsProvider.Builder(assumeRoleArn, roleSessionName)
                .withStsClient(stsClient)
                .build();
    }

    @Override
    public AWSCredentials getCredentials() {
        return this.credentials.getCredentials();
    }

    @Override
    public void refresh() {
        this.credentials.refresh();
    }
}

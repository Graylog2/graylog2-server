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


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.apache.ApacheHttpClient;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

public class AWSAuthFactoryTest {
    private AWSAuthFactory awsAuthFactory;

    @BeforeEach
    public void setUp() {
        awsAuthFactory = new AWSAuthFactory();
    }

    @Test
    public void testAutomaticAuth() {
        assertThat(awsAuthFactory.create(false, null, null, null, null))
                .isExactlyInstanceOf(DefaultCredentialsProvider.class);
    }

    @Test
    public void testAutomaticAuthIsFailingInCloudWithInvalidAccessKey() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() ->
                        awsAuthFactory.create(true, null, null, "secret", null))
                .withMessageContaining("Access key");
    }

    @Test
    public void testAutomaticAuthIsFailingInCloudWithInvalidSecretKey() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() ->
                        awsAuthFactory.create(true, null, "key", null, null))
                .withMessageContaining("Secret key");
    }


    @Test
    public void testKeySecret() {
        final AwsCredentialsProvider awsCredentialsProvider = awsAuthFactory.create(false, null, "key", "secret", null);
        assertThat(awsCredentialsProvider).isExactlyInstanceOf(StaticCredentialsProvider.class);
        assertThat("key").isEqualTo(awsCredentialsProvider.resolveCredentials().accessKeyId());
        assertThat("secret").isEqualTo(awsCredentialsProvider.resolveCredentials().secretAccessKey());
    }

    @Test
    public void testKeySecret_exceptionThrownWhenRequired() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() ->
                        awsAuthFactory.create(true, null, null, null, null))
                .withMessageContaining("Access key is required.");
    }

    @Test
    public void testCreateWithNullHttpClientBuilder() {
        final AwsCredentialsProvider awsCredentialsProvider = awsAuthFactory.create(false, null, "key", "secret", null, null);
        assertThat(awsCredentialsProvider).isExactlyInstanceOf(StaticCredentialsProvider.class);
        assertThat("key").isEqualTo(awsCredentialsProvider.resolveCredentials().accessKeyId());
    }

    @Test
    public void testSixArgOverload_withHttpClientBuilder_noAssumeRole_returnsStaticCredentials() {
        final ApacheHttpClient.Builder httpClientBuilder = ApacheHttpClient.builder();
        final AwsCredentialsProvider result = awsAuthFactory.create(false, null, "key", "secret", null, httpClientBuilder);
        assertThat(result).isExactlyInstanceOf(StaticCredentialsProvider.class);
        assertThat(result.resolveCredentials().accessKeyId()).isEqualTo("key");
        assertThat(result.resolveCredentials().secretAccessKey()).isEqualTo("secret");
    }

    @Test
    public void testSixArgOverload_withHttpClientBuilder_noAssumeRole_returnsDefaultCredentials() {
        final ApacheHttpClient.Builder httpClientBuilder = ApacheHttpClient.builder();
        final AwsCredentialsProvider result = awsAuthFactory.create(false, null, null, null, null, httpClientBuilder);
        assertThat(result).isExactlyInstanceOf(DefaultCredentialsProvider.class);
    }

    @Test
    public void testFiveArgOverload_delegatesToSixArg() {
        // The 5-arg create() should produce identical results to the 6-arg create() with null builder
        final AwsCredentialsProvider fiveArg = awsAuthFactory.create(false, null, "key", "secret", null);
        final AwsCredentialsProvider sixArg = awsAuthFactory.create(false, null, "key", "secret", null, null);
        assertThat(fiveArg).isExactlyInstanceOf(sixArg.getClass());
        assertThat(fiveArg.resolveCredentials().accessKeyId()).isEqualTo(sixArg.resolveCredentials().accessKeyId());
    }
}

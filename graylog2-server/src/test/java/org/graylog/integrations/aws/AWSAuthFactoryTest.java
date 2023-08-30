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


import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

public class AWSAuthFactoryTest {

    @Test
    public void testAutomaticAuth() {
        assertThat(AWSAuthFactory.create(false, null, null, null, null))
                .isExactlyInstanceOf(DefaultCredentialsProvider.class);
    }

    @Test
    public void testAutomaticAuthIsFailingInCloudWithInvalidAccessKey() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() ->
                        AWSAuthFactory.create(true, null, null, "secret", null))
                .withMessageContaining("Access key");
    }

    @Test
    public void testAutomaticAuthIsFailingInCloudWithInvalidSecretKey() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() ->
                        AWSAuthFactory.create(true, null, "key", null, null))
                .withMessageContaining("Secret key");
    }


    @Test
    public void testKeySecret() {
        final AwsCredentialsProvider awsCredentialsProvider = AWSAuthFactory.create(false, null, "key", "secret", null);
        assertThat(awsCredentialsProvider).isExactlyInstanceOf(StaticCredentialsProvider.class);
        assertThat("key").isEqualTo(awsCredentialsProvider.resolveCredentials().accessKeyId());
        assertThat("secret").isEqualTo(awsCredentialsProvider.resolveCredentials().secretAccessKey());
    }
}

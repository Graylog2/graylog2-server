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
import org.graylog2.Configuration;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class AWSAuthProviderTest {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private Configuration systemConfiguration;

    @Test
    public void encryptSecretKeyFromPluginConfigUsingSystemSecret() {
        when(systemConfiguration.getPasswordSecret()).thenReturn("encryptionKey123");
        final AWSPluginConfiguration config = AWSPluginConfiguration.createDefault()
                .toBuilder()
                .accessKey("MyAccessKey")
                .secretKey("aVerySecretKey", "encryptionKey123")
                .build();

        AWSAuthProvider authProvider = createForConfig(config);

        assertThat(authProvider.getCredentials().getAWSSecretKey()).isEqualTo("aVerySecretKey");
        assertThat(authProvider.getCredentials().getAWSAccessKeyId()).isEqualTo("MyAccessKey");
    }

    private AWSAuthProvider createForConfig(AWSPluginConfiguration config) {
        return new AWSAuthProvider(systemConfiguration, config);

    }
}

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
package org.graylog.aws.migrations;

import org.graylog2.Configuration;
import org.graylog2.migrations.Migration;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.security.AESTools;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class V20200505121200_EncryptAWSSecretKeyTest {
    public static final String PLUGIN_CONFIG_CLASS_NAME = "org.graylog.aws.config.AWSPluginConfiguration";
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private ClusterConfigService clusterConfigService;

    @Mock
    private Configuration configuration;

    private Migration migration;

    @Before
    public void setUp() {
        this.migration = new V20200505121200_EncryptAWSSecretKey(clusterConfigService, configuration);
    }

    @Test
    public void doesNotDoAnyThingForMissingPluginConfig() {
        mockExistingConfig(null);

        this.migration.upgrade();

        verify(clusterConfigService, never()).write(anyString(), any());
        verify(clusterConfigService, times(1)).write(any(V20200505121200_EncryptAWSSecretKey.MigrationCompleted.class));
    }

    @Test
    public void doesNotDoAnyThingForExistingPluginConfigWithoutSecretKey() {
        mockExistingConfig(V20200505121200_EncryptAWSSecretKey.LegacyAWSPluginConfiguration.create(
                true,
                "lookupRegions",
                "something",
                "",
                true
        ));

        this.migration.upgrade();

        verify(clusterConfigService, never()).write(anyString(), any());
        verify(clusterConfigService, times(1)).write(any(V20200505121200_EncryptAWSSecretKey.MigrationCompleted.class));
    }

    @Test
    public void encryptsSecretKeyIfPresent() {
        mockExistingConfig(V20200505121200_EncryptAWSSecretKey.LegacyAWSPluginConfiguration.create(
                true,
                "lookupRegions",
                "something",
                "verySecretKey",
                true
        ));
        when(configuration.getPasswordSecret()).thenReturn("systemSecret1234");

        this.migration.upgrade();

        final ArgumentCaptor<V20200505121200_EncryptAWSSecretKey.AWSPluginConfiguration> writtenConfigCaptor = ArgumentCaptor.forClass(
                V20200505121200_EncryptAWSSecretKey.AWSPluginConfiguration.class
        );
        verify(clusterConfigService, times(1)).write(eq(PLUGIN_CONFIG_CLASS_NAME), writtenConfigCaptor.capture());
        verify(clusterConfigService, times(1)).write(any(V20200505121200_EncryptAWSSecretKey.MigrationCompleted.class));
        final V20200505121200_EncryptAWSSecretKey.AWSPluginConfiguration writtenConfig = writtenConfigCaptor.getValue();

        assertThat(AESTools.decrypt(writtenConfig.encryptedSecretKey(), "systemSecret1234", writtenConfig.secretKeySalt()))
                .isEqualTo("verySecretKey");
    }

    private void mockExistingConfig(V20200505121200_EncryptAWSSecretKey.LegacyAWSPluginConfiguration config) {
        when(clusterConfigService.get(eq(PLUGIN_CONFIG_CLASS_NAME), any()))
                .thenReturn(config);
    }
}

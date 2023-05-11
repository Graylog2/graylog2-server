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
package org.graylog.aws.config;

import org.graylog2.Configuration;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.rest.resources.RestResourceBaseTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AWSConfigurationResourceTest extends RestResourceBaseTest {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private ClusterConfigService clusterConfigService;

    @Mock
    private Configuration systemConfiguration;

    private AWSConfigurationResource awsConfigurationResource;

    @Before
    public void setUp() {
        when(systemConfiguration.getPasswordSecret()).thenReturn("verySecret123456");
        this.awsConfigurationResource = new AWSConfigurationResource(clusterConfigService, systemConfiguration);
    }

    @Test
    public void updatesEverythingElseButSecretKeyAndSaltIfNotPresent() {
        mockPreviousConfig(sampleConfigWithoutSecretKey());
        final AWSPluginConfigurationUpdate update = AWSPluginConfigurationUpdate.create(
                true,
                "lookupRegions",
                "myAccessKey",
                null,
                true
        );

        this.awsConfigurationResource.updateConfig(update);

        final AWSPluginConfiguration writtenConfig = captureWrittenConfig();
        assertThat(writtenConfig).isEqualTo(AWSPluginConfiguration.create(
                true,
                "lookupRegions",
                "myAccessKey",
                null,
                null,
                true
        ));
    }

    @Test
    public void updatesPreviouslyMissingConfig() {
        mockPreviousConfig(AWSPluginConfiguration.createDefault());
        final AWSPluginConfigurationUpdate update = AWSPluginConfigurationUpdate.create(
                true,
                "lookupRegions",
                "myAccessKey",
                "aNewSecretKey",
                true
        );

        this.awsConfigurationResource.updateConfig(update);

        final AWSPluginConfiguration writtenConfig = captureWrittenConfig();
        assertThat(writtenConfig.lookupsEnabled()).isTrue();
        assertThat(writtenConfig.lookupRegions()).isEqualTo("lookupRegions");
        assertThat(writtenConfig.accessKey()).isEqualTo("myAccessKey");
        assertThat(writtenConfig.proxyEnabled()).isTrue();
        assertThat(writtenConfig.secretKey("verySecret123456")).isEqualTo("aNewSecretKey");
    }

    @Test
    public void updatesSecretKeyAndSaltIfPresentForPreviouslyMissingSecretKey() {
        mockPreviousConfig(sampleConfigWithoutSecretKey());
        final AWSPluginConfigurationUpdate update = AWSPluginConfigurationUpdate.create(
                true,
                "lookupRegions",
                "myAccessKey",
                "newSecretKey",
                true
        );

        this.awsConfigurationResource.updateConfig(update);

        final AWSPluginConfiguration writtenConfig = captureWrittenConfig();
        assertThat(writtenConfig.secretKey("verySecret123456")).isEqualTo("newSecretKey");
    }

    @Test
    public void updatesSecretKeyAndSaltIfPresentForPreexistingSecretKey() {
        mockPreviousConfig(sampleConfigWithSecretKey());
        final AWSPluginConfigurationUpdate update = AWSPluginConfigurationUpdate.create(
                true,
                "lookupRegions",
                "myAccessKey",
                "newSecretKey",
                true
        );

        this.awsConfigurationResource.updateConfig(update);

        final AWSPluginConfiguration writtenConfig = captureWrittenConfig();
        assertThat(writtenConfig.secretKey("verySecret123456")).isEqualTo("newSecretKey");
    }

    private void mockPreviousConfig(AWSPluginConfiguration previousConfig) {
        when(clusterConfigService.getOrDefault(eq(AWSPluginConfiguration.class), any(AWSPluginConfiguration.class)))
                .thenReturn(previousConfig);
    }

    private AWSPluginConfiguration captureWrittenConfig() {
        final ArgumentCaptor<AWSPluginConfiguration> writtenConfigCaptor = ArgumentCaptor.forClass(AWSPluginConfiguration.class);

        verify(clusterConfigService, times(1)).write(writtenConfigCaptor.capture());

        return writtenConfigCaptor.getValue();
    }

    private AWSPluginConfiguration sampleConfigWithoutSecretKey() {
        return AWSPluginConfiguration.create(
                false,
                "noLookupRegions",
                "",
                null,
                null,
                false
        );
    }

    private AWSPluginConfiguration sampleConfigWithSecretKey() {
        return AWSPluginConfiguration.create(
                false,
                "noLookupRegions",
                "",
                "foobar",
                "salt",
                false
        );
    }
}

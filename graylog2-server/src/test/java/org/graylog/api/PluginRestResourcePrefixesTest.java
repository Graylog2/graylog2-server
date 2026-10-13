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
package org.graylog.api;

import org.graylog.aws.config.AWSConfigurationResource;
import org.graylog.aws.inputs.cloudtrail.api.CloudTrailResource;
import org.graylog.integrations.aws.resources.AWSResource;
import org.graylog.integrations.aws.resources.KinesisSetupResource;
import org.graylog.plugins.views.search.rest.DashboardsResource;
import org.graylog.security.rest.CertificateRenewalResource;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PluginRestResourcePrefixesTest {
    private static final Set<Class<?>> RESOURCES = Set.of(
            AWSConfigurationResource.class,
            CloudTrailResource.class,
            AWSResource.class,
            KinesisSetupResource.class,
            CertificateRenewalResource.class,
            DashboardsResource.class
    );

    private final Map<Class<?>, String> prefixes = PluginRestResourcePrefixes.forResources(RESOURCES, "org.graylog", "org.graylog2");

    @Test
    void usesPackageOfRegisteringModule() {
        assertThat(prefixes).containsEntry(AWSConfigurationResource.class, "org.graylog.aws")
                .containsEntry(AWSResource.class, "org.graylog.integrations")
                .containsEntry(KinesisSetupResource.class, "org.graylog.integrations")
                .containsEntry(CertificateRenewalResource.class, "org.graylog2.shared.security");
    }

    @Test
    void usesModulePackageEvenIfResourceLivesElsewhere() {
        assertThat(prefixes).containsEntry(CloudTrailResource.class, "org.graylog.integrations");
    }

    @Test
    void doesNotPrefixSystemResourcesOfPluginModules() {
        assertThat(prefixes).doesNotContainKey(DashboardsResource.class);
    }
}

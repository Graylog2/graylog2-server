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
package org.graylog.datanode.opensearch.configuration.beans.impl;

import com.github.joschi.jadconfig.RepositoryException;
import com.github.joschi.jadconfig.ValidationException;
import org.assertj.core.api.Assertions;
import org.graylog.datanode.DatanodeTestUtils;
import org.graylog.datanode.OpensearchDistribution;
import org.graylog.datanode.configuration.DatanodeConfiguration;
import org.graylog.datanode.opensearch.configuration.OpensearchConfigurationParams;
import org.graylog.datanode.process.configuration.beans.DatanodeConfigurationPart;
import org.graylog2.security.jwt.IndexerJwtAuthToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

class OpensearchCommonConfigurationBeanTest {

    @Test
    void testBootstrapMemoryLockDisabledByDefault(@TempDir Path tempDir) throws ValidationException, RepositoryException {
        final DatanodeConfiguration datanodeConfiguration = mockDatanodeConfiguration(tempDir);

        final OpensearchCommonConfigurationBean bean = new OpensearchCommonConfigurationBean(
                DatanodeTestUtils.datanodeConfiguration(Map.of(), tempDir),
                datanodeConfiguration
        );

        final DatanodeConfigurationPart configurationPart = bean.buildConfigurationPart(new OpensearchConfigurationParams(tempDir));

        Assertions.assertThat(configurationPart.properties())
                .containsEntry("bootstrap.memory_lock", "false");
    }

    @Test
    void testBootstrapMemoryLockCanBeDisabled(@TempDir Path tempDir) throws ValidationException, RepositoryException {
        final DatanodeConfiguration datanodeConfiguration = mockDatanodeConfiguration(tempDir);

        final OpensearchCommonConfigurationBean bean = new OpensearchCommonConfigurationBean(
                DatanodeTestUtils.datanodeConfiguration(Map.of("opensearch_bootstrap_memory_lock", "false"), tempDir),
                datanodeConfiguration
        );

        final DatanodeConfigurationPart configurationPart = bean.buildConfigurationPart(new OpensearchConfigurationParams(tempDir));

        Assertions.assertThat(configurationPart.properties())
                .containsEntry("bootstrap.memory_lock", "false");
    }

    @Test
    void testBootstrapMemoryLockCanBeEnabled(@TempDir Path tempDir) throws ValidationException, RepositoryException {
        final DatanodeConfiguration datanodeConfiguration = mockDatanodeConfiguration(tempDir);

        final OpensearchCommonConfigurationBean bean = new OpensearchCommonConfigurationBean(
                DatanodeTestUtils.datanodeConfiguration(Map.of("opensearch_bootstrap_memory_lock", "true"), tempDir),
                datanodeConfiguration
        );

        final DatanodeConfigurationPart configurationPart = bean.buildConfigurationPart(new OpensearchConfigurationParams(tempDir));

        Assertions.assertThat(configurationPart.properties())
                .containsEntry("bootstrap.memory_lock", "true");
    }

    private DatanodeConfiguration mockDatanodeConfiguration(Path tempDir) {
        return new DatanodeConfiguration(
                () -> new OpensearchDistribution(tempDir, "2.19.5"),
                DatanodeTestUtils.tempDirectories(tempDir),
                100,
                IndexerJwtAuthToken.disabled()
        );
    }
}

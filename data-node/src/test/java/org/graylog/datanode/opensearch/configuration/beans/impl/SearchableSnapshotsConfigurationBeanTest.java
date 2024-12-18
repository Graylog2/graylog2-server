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

import com.github.joschi.jadconfig.JadConfig;
import com.github.joschi.jadconfig.RepositoryException;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.repositories.InMemoryRepository;
import org.assertj.core.api.Assertions;
import org.graylog.datanode.Configuration;
import org.graylog.datanode.configuration.OpensearchConfigurationException;
import org.graylog.datanode.configuration.S3RepositoryConfiguration;
import org.graylog.datanode.opensearch.configuration.OpensearchConfigurationParams;
import org.graylog.datanode.opensearch.configuration.OpensearchUsableSpace;
import org.graylog.datanode.process.configuration.beans.DatanodeConfigurationPart;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

class SearchableSnapshotsConfigurationBeanTest {

    @Test
    void testS3Repo(@TempDir Path tempDir) throws ValidationException, RepositoryException {
        final S3RepositoryConfiguration config = s3Configuration(Map.of(
                "s3_client_default_access_key", "user",
                "s3_client_default_secret_key", "password",
                "s3_client_default_endpoint", "http://localhost:9000"

        ));

        final SearchableSnapshotsConfigurationBean bean = new SearchableSnapshotsConfigurationBean(
                datanodeConfiguration(Map.of(
                        "node_search_cache_size", "10gb"
                )),
                config,
                () -> new OpensearchUsableSpace(tempDir, 20L * 1024 * 1024 * 1024));

        final DatanodeConfigurationPart configurationPart = bean.buildConfigurationPart(emptyBuildParams());

        Assertions.assertThat(configurationPart.nodeRoles())
                .contains(SearchableSnapshotsConfigurationBean.SEARCH_NODE_ROLE);

        Assertions.assertThat(configurationPart.keystoreItems())
                .containsKeys("s3.client.default.access_key", "s3.client.default.secret_key");

        Assertions.assertThat(configurationPart.properties())
                .containsKeys("s3.client.default.endpoint", "node.search.cache.size");
    }

    private OpensearchConfigurationParams emptyBuildParams() {
        return new OpensearchConfigurationParams(Collections.emptyList(), Collections.emptyMap());
    }

    @Test
    void testLocalFilesystemRepo(@TempDir Path tempDir) throws ValidationException, RepositoryException {
        // no s3 repo configuration properties given by the user
        final S3RepositoryConfiguration config = s3Configuration(Map.of());

        // only path_repo in general datanode configuration
        final SearchableSnapshotsConfigurationBean bean = new SearchableSnapshotsConfigurationBean(
                datanodeConfiguration(Map.of(
                        "path_repo", "/mnt/data/snapshots",
                        "node_search_cache_size", "10gb"
                )),
                config,
                () -> new OpensearchUsableSpace(tempDir, 20L * 1024 * 1024 * 1024));

        final DatanodeConfigurationPart configurationPart = bean.buildConfigurationPart(emptyBuildParams());

        Assertions.assertThat(configurationPart.nodeRoles())
                .contains(SearchableSnapshotsConfigurationBean.SEARCH_NODE_ROLE);

        Assertions.assertThat(configurationPart.keystoreItems())
                .isEmpty();

        Assertions.assertThat(configurationPart.properties())
                .containsEntry("path.repo", "/mnt/data/snapshots")
                .containsEntry("node.search.cache.size", "10gb");
    }

    @Test
    void testNoSnapshotConfiguration(@TempDir Path tempDir) throws ValidationException, RepositoryException {
        // no s3 repo configuration properties given by the user
        final S3RepositoryConfiguration config = s3Configuration(Map.of());

        // only path_repo in general datanode configuration
        final SearchableSnapshotsConfigurationBean bean = new SearchableSnapshotsConfigurationBean(
                datanodeConfiguration(Map.of(
                        "node_search_cache_size", "10gb"
                )),
                config,
                () -> new OpensearchUsableSpace(tempDir, 20L * 1024 * 1024 * 1024));

        final DatanodeConfigurationPart configurationPart = bean.buildConfigurationPart(emptyBuildParams());

        Assertions.assertThat(configurationPart.nodeRoles())
                .isEmpty(); // no search role should be provided

        Assertions.assertThat(configurationPart.keystoreItems())
                .isEmpty();

        Assertions.assertThat(configurationPart.properties())
                .isEmpty(); // no cache configuration should be provided
    }

    @Test
    void testCacheSizeValidation(@TempDir Path tempDir) throws ValidationException, RepositoryException {
        final S3RepositoryConfiguration config = s3Configuration(Map.of(
                "s3_client_default_access_key", "user",
                "s3_client_default_secret_key", "password",
                "s3_client_default_endpoint", "http://localhost:9000"

        ));

        final SearchableSnapshotsConfigurationBean bean = new SearchableSnapshotsConfigurationBean(
                datanodeConfiguration(Map.of(
                        "node_search_cache_size", "10gb"
                )),
                config,
                () -> new OpensearchUsableSpace(tempDir, 8L * 1024 * 1024 * 1024));

        // 10GB cache requested on 8GB of free space, needs to throw an exception!
        Assertions.assertThatThrownBy(() -> bean.buildConfigurationPart(emptyBuildParams()))
                .isInstanceOf(OpensearchConfigurationException.class)
                .hasMessageContaining("There is not enough usable space for the node search cache. Your system has only 8gb available");


    }

    private S3RepositoryConfiguration s3Configuration(Map<String, String> properties) throws RepositoryException, ValidationException {
        final S3RepositoryConfiguration configuration = new S3RepositoryConfiguration();
        new JadConfig(new InMemoryRepository(properties), configuration).process();
        return configuration;
    }

    private Configuration datanodeConfiguration(Map<String, String> properties) throws RepositoryException, ValidationException {
        final Configuration configuration = new Configuration();
        final InMemoryRepository mandatoryProps = new InMemoryRepository(Map.of(
                "password_secret", "thisisverysecretpassword"
        ));
        new JadConfig(List.of(mandatoryProps, new InMemoryRepository(properties)), configuration).process();
        return configuration;
    }
}

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
import org.graylog.datanode.DatanodeTestUtils;
import org.graylog.datanode.configuration.DatanodeDirectories;
import org.graylog.datanode.configuration.OpensearchConfigurationException;
import org.graylog.datanode.configuration.snapshots.AzureRepositoryConfiguration;
import org.graylog.datanode.configuration.snapshots.FsRepositoryConfiguration;
import org.graylog.datanode.configuration.snapshots.GCSRepositoryConfiguration;
import org.graylog.datanode.configuration.snapshots.HdfsRepositoryConfiguration;
import org.graylog.datanode.configuration.snapshots.S3RepositoryConfiguration;
import org.graylog.datanode.opensearch.configuration.OpensearchConfigurationParams;
import org.graylog.datanode.opensearch.configuration.OpensearchUsableSpace;
import org.graylog.datanode.process.configuration.beans.DatanodeConfigurationPart;
import org.graylog.datanode.process.configuration.beans.OpensearchKeystoreItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

class SearchableSnapshotsConfigurationBeanTest {

    @Test
    void testS3Repo(@TempDir Path tempDir) throws ValidationException, RepositoryException {
        final S3RepositoryConfiguration config = s3Configuration(Map.of(
                "s3_client_default_access_key", "user",
                "s3_client_default_secret_key", "password",
                "s3_client_default_endpoint", "http://localhost:9000"

        ));

        final SearchableSnapshotsConfigurationBean bean = new SearchableSnapshotsConfigurationBean(
                DatanodeTestUtils.datanodeConfiguration(Map.of(
                        "node_search_cache_size", "10gb"
                ), tempDir),
                datanodeDirectories(tempDir),
                Set.of(config),
                () -> new OpensearchUsableSpace(tempDir, 20L * 1024 * 1024 * 1024));

        final DatanodeConfigurationPart configurationPart = bean.buildConfigurationPart(emptyBuildParams(tempDir));

        Assertions.assertThat(configurationPart.nodeRoles())
                .contains(OpensearchNodeRole.SEARCH);

        Assertions.assertThat(configurationPart.keystoreItems())
                .map(OpensearchKeystoreItem::key)
                .contains("s3.client.default.access_key", "s3.client.default.secret_key");

        Assertions.assertThat(configurationPart.properties())
                .containsKeys("s3.client.default.endpoint", "node.search.cache.size");
    }

    private DatanodeDirectories datanodeDirectories(Path tempDir) {
        return new DatanodeDirectories(tempDir, tempDir, tempDir, tempDir);
    }

    @Test
    void testGoogleCloudStorage(@TempDir Path tempDir) throws ValidationException, RepositoryException, IOException {

        final Path credentialsFile = Files.createTempFile(tempDir, "gcs-credentials", ".json");
        // let's use the filename only. This should be automatically resolved against the datanode config source directory
        final String credentialsFileName = credentialsFile.getFileName().toString();
        final GCSRepositoryConfiguration gcsRepositoryConfiguration = gcsConfiguration(Map.of(
                "gcs_credentials_file", credentialsFileName
        ));

        final SearchableSnapshotsConfigurationBean bean = new SearchableSnapshotsConfigurationBean(
                DatanodeTestUtils.datanodeConfiguration(Map.of(
                        "node_search_cache_size", "10gb"
                ), tempDir),
                datanodeDirectories(tempDir),
                Set.of(gcsRepositoryConfiguration),
                () -> new OpensearchUsableSpace(tempDir, 20L * 1024 * 1024 * 1024));

        final DatanodeConfigurationPart configurationPart = bean.buildConfigurationPart(emptyBuildParams(tempDir));

        Assertions.assertThat(configurationPart.nodeRoles())
                .contains(OpensearchNodeRole.SEARCH);

        Assertions.assertThat(configurationPart.keystoreItems())
                .hasSize(1)
                .map(OpensearchKeystoreItem::key)
                .contains("gcs.client.default.credentials_file");

        Assertions.assertThat(configurationPart.properties())
                .containsEntry("node.search.cache.size", "10gb");
    }

    @Test
    void testHadoopDistributedFileStorage(@TempDir Path tempDir) throws ValidationException, RepositoryException {
        final HdfsRepositoryConfiguration hdfsConfiguration = hdfsConfiguration(Map.of(
                "hdfs_repository_enabled", "true"
        ));

        final SearchableSnapshotsConfigurationBean bean = new SearchableSnapshotsConfigurationBean(
                DatanodeTestUtils.datanodeConfiguration(Map.of(
                        "node_search_cache_size", "10gb"
                ), tempDir),
                datanodeDirectories(tempDir),
                Set.of(hdfsConfiguration),
                () -> new OpensearchUsableSpace(tempDir, 20L * 1024 * 1024 * 1024));

        final DatanodeConfigurationPart configurationPart = bean.buildConfigurationPart(emptyBuildParams(tempDir));

        Assertions.assertThat(configurationPart.nodeRoles())
                .contains(OpensearchNodeRole.SEARCH);

        Assertions.assertThat(configurationPart.properties())
                .containsEntry("node.search.cache.size", "10gb");
    }

    @Test
    void testAzureBlobStorage(@TempDir Path tempDir) throws ValidationException, RepositoryException {

        final AzureRepositoryConfiguration azureConfiguration = azureConfiguration(Map.of(
                "azure_client_default_account", "asdfgh",
                "azure_client_default_key", "12345"
        ));

        final SearchableSnapshotsConfigurationBean bean = new SearchableSnapshotsConfigurationBean(
                DatanodeTestUtils.datanodeConfiguration(Map.of(
                        "node_search_cache_size", "10gb"
                ), tempDir),
                datanodeDirectories(tempDir),
                Set.of(azureConfiguration),
                () -> new OpensearchUsableSpace(tempDir, 20L * 1024 * 1024 * 1024));

        final DatanodeConfigurationPart configurationPart = bean.buildConfigurationPart(emptyBuildParams(tempDir));

        Assertions.assertThat(configurationPart.nodeRoles())
                .contains(OpensearchNodeRole.SEARCH);

        Assertions.assertThat(configurationPart.properties())
                .containsEntry("node.search.cache.size", "10gb");

        Assertions.assertThat(configurationPart.keystoreItems())
                .hasSize(2)
                .extracting(OpensearchKeystoreItem::key)
                .contains("azure.client.default.account", "azure.client.default.key");
    }

    private OpensearchConfigurationParams emptyBuildParams(Path tempDir) {
        return new OpensearchConfigurationParams(Collections.emptyList(), Collections.emptyMap(), tempDir);
    }

    @Test
    void testLocalFilesystemRepo(@TempDir Path tempDir) throws ValidationException, RepositoryException, IOException {

        final String snapshotsPath = Files.createDirectory(tempDir.resolve("snapshots")).toAbsolutePath().toString();
        final FsRepositoryConfiguration config = fsConfiguration(snapshotsPath);


        // only path_repo in general datanode configuration
        final SearchableSnapshotsConfigurationBean bean = new SearchableSnapshotsConfigurationBean(
                DatanodeTestUtils.datanodeConfiguration(Map.of(
                        "node_search_cache_size", "10gb"
                ), tempDir),
                datanodeDirectories(tempDir),
                Set.of(config),
                () -> new OpensearchUsableSpace(tempDir, 20L * 1024 * 1024 * 1024));

        final DatanodeConfigurationPart configurationPart = bean.buildConfigurationPart(emptyBuildParams(tempDir));

        Assertions.assertThat(configurationPart.nodeRoles())
                .contains(OpensearchNodeRole.SEARCH);

        Assertions.assertThat(configurationPart.keystoreItems())
                .isEmpty();

        Assertions.assertThat(configurationPart.properties())
                .containsEntry("path.repo", snapshotsPath)
                .containsEntry("node.search.cache.size", "10gb");
    }

    @Test
    void testNoSnapshotConfiguration(@TempDir Path tempDir) throws ValidationException, RepositoryException {


        // only path_repo in general datanode configuration
        final SearchableSnapshotsConfigurationBean bean = new SearchableSnapshotsConfigurationBean(
                DatanodeTestUtils.datanodeConfiguration(Map.of(
                        "node_search_cache_size", "10gb"
                ), tempDir),
                datanodeDirectories(tempDir),
                Collections.emptySet(),
                () -> new OpensearchUsableSpace(tempDir, 20L * 1024 * 1024 * 1024));

        final DatanodeConfigurationPart configurationPart = bean.buildConfigurationPart(emptyBuildParams(tempDir));

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
                DatanodeTestUtils.datanodeConfiguration(Map.of(
                        "node_search_cache_size", "10gb"
                ), tempDir),
                datanodeDirectories(tempDir),
                Set.of(config),
                () -> new OpensearchUsableSpace(tempDir, 8L * 1024 * 1024 * 1024));

        // 10GB cache requested on 8GB of free space, needs to throw an exception!
        Assertions.assertThatThrownBy(() -> bean.buildConfigurationPart(emptyBuildParams(tempDir)))
                .isInstanceOf(OpensearchConfigurationException.class)
                .hasMessageContaining("There is not enough usable space for the node search cache. Your system has only 8gb available");
    }

    @Test
    void testRepoConfigWithoutSearchRole(@TempDir Path tempDir) throws ValidationException, RepositoryException, IOException {

        final String snapshotsPath = Files.createDirectory(tempDir.resolve("snapshots")).toAbsolutePath().toString();
        final FsRepositoryConfiguration fsRepo = fsConfiguration(snapshotsPath);

        // only path_repo in general datanode configuration
        final SearchableSnapshotsConfigurationBean bean = new SearchableSnapshotsConfigurationBean(
                DatanodeTestUtils.datanodeConfiguration(Map.of(
                        "node_roles", "cluster_manager,data,ingest,remote_cluster_client",
                        "path_repo", snapshotsPath,
                        "node_search_cache_size", "10gb"
                ), tempDir),
                datanodeDirectories(tempDir),
                Set.of(fsRepo),
                () -> new OpensearchUsableSpace(tempDir, 20L * 1024 * 1024 * 1024));

        final DatanodeConfigurationPart configurationPart = bean.buildConfigurationPart(emptyBuildParams(tempDir));

        Assertions.assertThat(configurationPart.nodeRoles())
                .isEmpty(); // no search role should be provided, we have to use only those that are given in the configuration

        Assertions.assertThat(configurationPart.properties())
                .containsEntry("path.repo", snapshotsPath)
                .doesNotContainEntry("node.search.cache.size", "10gb");
    }

    private AzureRepositoryConfiguration azureConfiguration(Map<String, String> properties) throws ValidationException, RepositoryException {
        final AzureRepositoryConfiguration configuration = new AzureRepositoryConfiguration();
        new JadConfig(new InMemoryRepository(properties), configuration).process();
        return configuration;
    }

    private FsRepositoryConfiguration fsConfiguration(String snapshotsPath) throws ValidationException, RepositoryException {
        final FsRepositoryConfiguration configuration = new FsRepositoryConfiguration();
        new JadConfig(new InMemoryRepository(Map.of("path_repo", snapshotsPath)), configuration).process();
        return configuration;
    }

    private GCSRepositoryConfiguration gcsConfiguration(Map<String, String> properties) throws ValidationException, RepositoryException {
        final GCSRepositoryConfiguration configuration = new GCSRepositoryConfiguration();
        new JadConfig(new InMemoryRepository(properties), configuration).process();
        return configuration;
    }

    private S3RepositoryConfiguration s3Configuration(Map<String, String> properties) throws RepositoryException, ValidationException {
        final S3RepositoryConfiguration configuration = new S3RepositoryConfiguration();
        new JadConfig(new InMemoryRepository(properties), configuration).process();
        return configuration;
    }

    private HdfsRepositoryConfiguration hdfsConfiguration(Map<String, String> properties) throws RepositoryException, ValidationException {
        final HdfsRepositoryConfiguration configuration = new HdfsRepositoryConfiguration();
        new JadConfig(new InMemoryRepository(properties), configuration).process();
        return configuration;
    }
}

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
import org.graylog.datanode.OpensearchDistribution;
import org.graylog.datanode.configuration.DatanodeConfiguration;
import org.graylog.datanode.configuration.DatanodeDirectories;
import org.graylog.datanode.configuration.OpensearchConfigurationException;
import org.graylog.datanode.configuration.snapshots.AzureRepositoryConfiguration;
import org.graylog.datanode.configuration.snapshots.FsRepositoryConfiguration;
import org.graylog.datanode.configuration.snapshots.GCSRepositoryConfiguration;
import org.graylog.datanode.configuration.snapshots.HdfsRepositoryConfiguration;
import org.graylog.datanode.configuration.snapshots.LocalRepositoryConfigurationProvider;
import org.graylog.datanode.configuration.snapshots.RepositoryConfiguration;
import org.graylog.datanode.configuration.snapshots.RepositoryConfigurationProvider;
import org.graylog.datanode.configuration.snapshots.S3RepositoryConfiguration;
import org.graylog.datanode.opensearch.configuration.OpensearchConfigurationParams;
import org.graylog.datanode.opensearch.configuration.OpensearchUsableSpace;
import org.graylog.datanode.process.configuration.beans.DatanodeConfigurationPart;
import org.graylog.datanode.process.configuration.beans.OpensearchKeystoreItem;
import org.graylog2.security.jwt.IndexerJwtAuthToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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

        final DatanodeConfiguration datanodeConfiguration = datanodeConfiguration(tempDir);
        final SearchableSnapshotsConfigurationBean bean = new SearchableSnapshotsConfigurationBean(
                DatanodeTestUtils.datanodeConfiguration(Map.of(
                        "node_search_cache_size", "10gb"
                ), tempDir),
                datanodeConfiguration,
                repositories(config),
                () -> new OpensearchUsableSpace(tempDir, 20L * 1024 * 1024 * 1024));

        final DatanodeConfigurationPart configurationPart = bean.buildConfigurationPart(emptyBuildParams(tempDir));

        Assertions.assertThat(configurationPart.nodeRoles())
                .contains(datanodeConfiguration.opensearchDistribution().distributionProperties().searchableSnapshotsRole());

        Assertions.assertThat(configurationPart.keystoreItems())
                .map(OpensearchKeystoreItem::key)
                .contains("s3.client.default.access_key", "s3.client.default.secret_key");

        Assertions.assertThat(configurationPart.properties())
                .containsKeys("s3.client.default.endpoint", "node.search.cache.size");
    }

    private DatanodeConfiguration datanodeConfiguration(Path tempDir) {
        return new DatanodeConfiguration(new OpensearchDistribution(tempDir, "2.19.6"), new DatanodeDirectories(tempDir, tempDir, tempDir, tempDir), 100, IndexerJwtAuthToken.disabled());
    }

    @Test
    void testGoogleCloudStorage(@TempDir Path tempDir) throws ValidationException, RepositoryException, IOException {

        final Path credentialsFile = Files.createTempFile(tempDir, "gcs-credentials", ".json");
        // let's use the filename only. This should be automatically resolved against the datanode config source directory
        final String credentialsFileName = credentialsFile.getFileName().toString();
        final GCSRepositoryConfiguration gcsRepositoryConfiguration = gcsConfiguration(Map.of(
                "gcs_credentials_file", credentialsFileName
        ));

        final DatanodeConfiguration datanodeConfiguration = datanodeConfiguration(tempDir);
        final SearchableSnapshotsConfigurationBean bean = new SearchableSnapshotsConfigurationBean(
                DatanodeTestUtils.datanodeConfiguration(Map.of(
                        "node_search_cache_size", "10gb"
                ), tempDir),
                datanodeConfiguration,
                repositories(gcsRepositoryConfiguration),
                () -> new OpensearchUsableSpace(tempDir, 20L * 1024 * 1024 * 1024));

        final DatanodeConfigurationPart configurationPart = bean.buildConfigurationPart(emptyBuildParams(tempDir));

        Assertions.assertThat(configurationPart.nodeRoles())
                .contains(datanodeConfiguration.opensearchDistribution().distributionProperties().searchableSnapshotsRole());

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

        final DatanodeConfiguration datanodeConfiguration = datanodeConfiguration(tempDir);
        final SearchableSnapshotsConfigurationBean bean = new SearchableSnapshotsConfigurationBean(
                DatanodeTestUtils.datanodeConfiguration(Map.of(
                        "node_search_cache_size", "10gb"
                ), tempDir),
                datanodeConfiguration,
                repositories(hdfsConfiguration),
                () -> new OpensearchUsableSpace(tempDir, 20L * 1024 * 1024 * 1024));

        final DatanodeConfigurationPart configurationPart = bean.buildConfigurationPart(emptyBuildParams(tempDir));

        Assertions.assertThat(configurationPart.nodeRoles())
                .contains(datanodeConfiguration.opensearchDistribution().distributionProperties().searchableSnapshotsRole());

        Assertions.assertThat(configurationPart.properties())
                .containsEntry("node.search.cache.size", "10gb");
    }

    @Test
    void testAzureBlobStorage(@TempDir Path tempDir) throws ValidationException, RepositoryException {

        final AzureRepositoryConfiguration azureConfiguration = azureConfiguration(Map.of(
                "azure_client_default_account", "asdfgh",
                "azure_client_default_key", "12345"
        ));

        final DatanodeConfiguration datanodeConfiguration = datanodeConfiguration(tempDir);
        final SearchableSnapshotsConfigurationBean bean = new SearchableSnapshotsConfigurationBean(
                DatanodeTestUtils.datanodeConfiguration(Map.of(
                        "node_search_cache_size", "10gb"
                ), tempDir),
                datanodeConfiguration,
                repositories(azureConfiguration),
                () -> new OpensearchUsableSpace(tempDir, 20L * 1024 * 1024 * 1024));

        final DatanodeConfigurationPart configurationPart = bean.buildConfigurationPart(emptyBuildParams(tempDir));

        Assertions.assertThat(configurationPart.nodeRoles())
                .contains(datanodeConfiguration.opensearchDistribution().distributionProperties().searchableSnapshotsRole());

        Assertions.assertThat(configurationPart.properties())
                .containsEntry("node.search.cache.size", "10gb");

        Assertions.assertThat(configurationPart.keystoreItems())
                .hasSize(2)
                .extracting(OpensearchKeystoreItem::key)
                .contains("azure.client.default.account", "azure.client.default.key");
    }

    private OpensearchConfigurationParams emptyBuildParams(Path tempDir) {
        return new OpensearchConfigurationParams(DatanodeTestUtils.mockDatanodeConfiguration(tempDir), tempDir);
    }

    @Test
    void testLocalFilesystemRepo(@TempDir Path tempDir) throws ValidationException, RepositoryException, IOException {

        final String snapshotsPath = Files.createDirectory(tempDir.resolve("snapshots")).toAbsolutePath().toString();
        final FsRepositoryConfiguration config = fsConfiguration(snapshotsPath);


        // only path_repo in general datanode configuration
        final DatanodeConfiguration datanodeConfiguration = datanodeConfiguration(tempDir);
        final SearchableSnapshotsConfigurationBean bean = new SearchableSnapshotsConfigurationBean(
                DatanodeTestUtils.datanodeConfiguration(Map.of(
                        "node_search_cache_size", "10gb"
                ), tempDir),
                datanodeConfiguration,
                repositories(config),
                () -> new OpensearchUsableSpace(tempDir, 20L * 1024 * 1024 * 1024));

        final DatanodeConfigurationPart configurationPart = bean.buildConfigurationPart(emptyBuildParams(tempDir));

        Assertions.assertThat(configurationPart.nodeRoles())
                .contains(datanodeConfiguration.opensearchDistribution().distributionProperties().searchableSnapshotsRole());

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
                datanodeConfiguration(tempDir),
                repositories(),
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
                datanodeConfiguration(tempDir),
                repositories(config),
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
                datanodeConfiguration(tempDir),
                repositories(fsRepo),
                () -> new OpensearchUsableSpace(tempDir, 20L * 1024 * 1024 * 1024));

        final DatanodeConfigurationPart configurationPart = bean.buildConfigurationPart(emptyBuildParams(tempDir));

        Assertions.assertThat(configurationPart.nodeRoles())
                .isEmpty(); // no search role should be provided, we have to use only those that are given in the configuration

        Assertions.assertThat(configurationPart.properties())
                .containsEntry("path.repo", snapshotsPath)
                .doesNotContainEntry("node.search.cache.size", "10gb");
    }

    @Test
    void testMultipleFilesystemRepositories(@TempDir Path tempDir) throws ValidationException, RepositoryException, IOException {
        final Path first = Files.createDirectory(tempDir.resolve("first"));
        final Path second = Files.createDirectory(tempDir.resolve("second"));
        final Path third = Files.createDirectory(tempDir.resolve("third"));

        final RepositoryConfigurationProvider localProvider = new LocalRepositoryConfigurationProvider(List.of(fsConfiguration(first.toString())));
        final RepositoryConfigurationProvider otherProvider = () -> List.of(
                new FsRepositoryConfiguration(List.of(second, third)),
                new FsRepositoryConfiguration(List.of(first)) // duplicate path, should be listed only once
        );
        final SearchableSnapshotsConfigurationBean bean = snapshotsBean(tempDir, Set.of(localProvider, otherProvider));

        final DatanodeConfigurationPart configurationPart = bean.buildConfigurationPart(emptyBuildParams(tempDir));

        Assertions.assertThat(configurationPart.properties().get("path.repo").split(","))
                .containsExactlyInAnyOrder(first.toString(), second.toString(), third.toString());
    }

    @Test
    void testMultipleS3Clients(@TempDir Path tempDir) throws ValidationException, RepositoryException {
        final S3RepositoryConfiguration defaultClient = s3Configuration(Map.of(
                "s3_client_default_access_key", "user",
                "s3_client_default_secret_key", "password",
                "s3_client_default_endpoint", "http://localhost:9000"
        ));
        final S3RepositoryConfiguration otherClient = new S3RepositoryConfiguration("other", "other-user", "other-password", "https", "https://s3.example.com", "eu-west-1", false);

        final SearchableSnapshotsConfigurationBean bean = snapshotsBean(tempDir, repositories(defaultClient, otherClient));

        final DatanodeConfigurationPart configurationPart = bean.buildConfigurationPart(emptyBuildParams(tempDir));

        Assertions.assertThat(configurationPart.properties())
                .containsEntry("s3.client.default.endpoint", "http://localhost:9000")
                .containsEntry("s3.client.other.endpoint", "https://s3.example.com")
                .containsEntry("s3.client.other.region", "eu-west-1")
                .containsEntry("s3.client.other.path_style_access", "false");

        Assertions.assertThat(configurationPart.keystoreItems())
                .extracting(OpensearchKeystoreItem::key)
                .containsExactlyInAnyOrder(
                        "s3.client.default.access_key", "s3.client.default.secret_key",
                        "s3.client.other.access_key", "s3.client.other.secret_key");
    }

    @Test
    void testConflictingClientNames(@TempDir Path tempDir) throws ValidationException, RepositoryException {
        final S3RepositoryConfiguration first = new S3RepositoryConfiguration("same", "user", "password", "http", "http://localhost:9000", "us-east-2", true);
        final S3RepositoryConfiguration second = new S3RepositoryConfiguration("same", "user", "password", "http", "http://localhost:9001", "us-east-2", true);

        final SearchableSnapshotsConfigurationBean bean = snapshotsBean(tempDir, repositories(first, second));

        Assertions.assertThatThrownBy(() -> bean.buildConfigurationPart(emptyBuildParams(tempDir)))
                .isInstanceOf(OpensearchConfigurationException.class)
                .hasMessageContaining("s3.client.same.endpoint");
    }

    private SearchableSnapshotsConfigurationBean snapshotsBean(Path tempDir, Set<RepositoryConfigurationProvider> providers) throws ValidationException, RepositoryException {
        return new SearchableSnapshotsConfigurationBean(
                DatanodeTestUtils.datanodeConfiguration(Map.of(
                        "node_search_cache_size", "10gb"
                ), tempDir),
                datanodeConfiguration(tempDir),
                providers,
                () -> new OpensearchUsableSpace(tempDir, 20L * 1024 * 1024 * 1024));
    }

    private Set<RepositoryConfigurationProvider> repositories(RepositoryConfiguration... repositories) {
        return Set.of(new LocalRepositoryConfigurationProvider(List.of(repositories)));
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

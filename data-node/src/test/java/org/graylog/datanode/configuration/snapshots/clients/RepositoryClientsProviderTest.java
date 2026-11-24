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
package org.graylog.datanode.configuration.snapshots.clients;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.graylog.datanode.configuration.snapshots.RepositoryConfiguration;
import org.graylog.datanode.process.configuration.beans.OpensearchKeystoreFileContentItem;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog2.database.MongoCollections;
import org.graylog2.security.encryption.EncryptedValue;
import org.graylog2.security.encryption.EncryptedValueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@ExtendWith(MongoDBExtension.class)
class RepositoryClientsProviderTest {

    private static final String CLIENT = "repository_archive-eu_7f3a";

    private final EncryptedValueService encryptedValueService = new EncryptedValueService("1234567890abcdef");
    private MongoCollections mongoCollections;
    private MongoCollection<Document> records;
    private RepositoryClientsProvider provider;

    @BeforeEach
    void setUp(MongoCollections mongoCollections) {
        this.mongoCollections = mongoCollections;
        records = mongoCollections.nonEntityCollection(RepositoryCredentialsDocument.COLLECTION_NAME, Document.class);
        provider = provider(encryptedValueService);
    }

    @Test
    void noRecords() {
        assertThat(provider.get()).isEmpty();
    }

    @Test
    void recordsWithoutClientNameAreIgnored() {
        records.insertOne(new Document("repository_name", "default-client").append("credentials", s3KeySecret()));

        assertThat(provider.get()).isEmpty();
    }

    @Test
    void s3CompatibleStorage() {
        insert("Archive.EU", CLIENT, s3KeySecret().append("endpoint_uri", "http://user:password@minio.local:9000/s3/"));

        final RepositoryConfiguration client = single(provider.get());
        assertThat(client.isRepositoryEnabled()).isTrue();
        // User info of the endpoint isn't used for authentication, it doesn't end up in the configuration
        assertThat(client.opensearchProperties()).isEqualTo(Map.of(
                "s3.client.repository_archive-eu_7f3a.region", "us-east-1",
                "s3.client.repository_archive-eu_7f3a.protocol", "http",
                "s3.client.repository_archive-eu_7f3a.endpoint", "minio.local:9000/s3",
                "s3.client.repository_archive-eu_7f3a.path_style_access", "true"));
        assertThat(client.keystoreItems(null))
                .extracting("key", "secret")
                .containsExactlyInAnyOrder(
                        tuple("s3.client.repository_archive-eu_7f3a.access_key", "access"),
                        tuple("s3.client.repository_archive-eu_7f3a.secret_key", "secret"));
    }

    @Test
    void awsWithAutomaticAuthenticationAndAssumedRole() {
        insert("Archive.EU", CLIENT, new Document("type", "s3").append("auth_mode", "automatic")
                .append("aws_region", "cn-north-1")
                .append("aws_assume_role_arn", "arn:aws-cn:iam::123456789012:role/warm"));

        final RepositoryConfiguration client = single(provider.get());
        assertThat(client.opensearchProperties()).isEqualTo(Map.of(
                "s3.client.repository_archive-eu_7f3a.region", "cn-north-1",
                "s3.client.repository_archive-eu_7f3a.protocol", "https",
                "s3.client.repository_archive-eu_7f3a.endpoint", "s3.cn-north-1.amazonaws.com.cn",
                "s3.client.repository_archive-eu_7f3a.path_style_access", "false"));
        // Without keys, opensearch uses the AWS credentials of the host. The session name tells the repository.
        assertThat(client.keystoreItems(null))
                .extracting("key", "secret")
                .containsExactlyInAnyOrder(
                        tuple("s3.client.repository_archive-eu_7f3a.role_arn", "arn:aws-cn:iam::123456789012:role/warm"),
                        tuple("s3.client.repository_archive-eu_7f3a.role_session_name", "graylog-archive-eu"));
    }

    @Test
    void recordsWithoutTypeAreS3Credentials() {
        final Document credentials = s3KeySecret();
        credentials.remove("type");
        insert("legacy", CLIENT, credentials);

        assertThat(single(provider.get()).opensearchProperties()).containsKey("s3.client.repository_archive-eu_7f3a.endpoint");
    }

    @Test
    void gcsServiceAccountKeyGoesToTheKeystoreAsFileContent() {
        insert("gcs", CLIENT, new Document("type", "gcs").append("auth_mode", "service_account_key")
                .append("service_account_key", encrypted("{\"type\": \"service_account\"}")));

        final RepositoryConfiguration client = single(provider.get());
        assertThat(client.opensearchProperties()).isEqualTo(Map.of(
                "gcs.client.repository_archive-eu_7f3a.endpoint", "https://storage.googleapis.com"));
        assertThat(client.keystoreItems(null))
                .singleElement()
                .usingRecursiveComparison()
                .isEqualTo(new OpensearchKeystoreFileContentItem("gcs.client.repository_archive-eu_7f3a.credentials_file",
                        "{\"type\": \"service_account\"}".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void gcsWithAutomaticAuthenticationHasNoSecrets() {
        insert("gcs", CLIENT, new Document("type", "gcs").append("auth_mode", "automatic"));

        final RepositoryConfiguration client = single(provider.get());
        assertThat(client.opensearchProperties()).containsOnlyKeys("gcs.client.repository_archive-eu_7f3a.endpoint");
        assertThat(client.keystoreItems(null)).isEmpty();
    }

    @Test
    void azureAccountKey() {
        insert("azure", CLIENT, new Document("type", "azure").append("auth_mode", "account_key")
                .append("account_name", "account").append("account_key", encrypted("key")));

        final RepositoryConfiguration client = single(provider.get());
        assertThat(client.opensearchProperties()).isEqualTo(Map.of(
                "azure.client.repository_archive-eu_7f3a.endpoint_suffix", "core.windows.net"));
        assertThat(client.keystoreItems(null))
                .extracting("key", "secret")
                .containsExactlyInAnyOrder(
                        tuple("azure.client.repository_archive-eu_7f3a.account", "account"),
                        tuple("azure.client.repository_archive-eu_7f3a.key", "key"));
    }

    @Test
    void secretsDoNotEndUpInTheDescriptionOfTheClient() {
        insert("minio", CLIENT, s3KeySecret().append("endpoint_uri", "http://minio.local:9000"));

        assertThat(single(provider.get()).toString()).contains("minio.local:9000").doesNotContain("=access", "=secret");
    }

    @Test
    void repositoriesGetTheirOwnClients() {
        insert("first", "repository_first_0001", s3KeySecret());
        insert("second", "repository_second_0002", s3KeySecret());

        assertThat(provider.get())
                .flatExtracting(client -> client.keystoreItems(null))
                .extracting("key")
                .contains("s3.client.repository_first_0001.access_key", "s3.client.repository_second_0002.access_key");
    }

    @Test
    void unusableRecordsDoNotAffectOthers() {
        insert("undecodable", "repository_undecodable_0001", new Document("type", "s3").append("aws_region", new Document()));
        insert("unknown-type", "repository_unknown-type_0002", new Document("type", "hdfs"));
        insert("external-id", "repository_external-id_0003", s3KeySecret().append("aws_external_id", "external"));
        insert("azure-automatic", "repository_azure-automatic_0004", new Document("type", "azure").append("auth_mode", "automatic"));
        insert("no-credentials", "repository_no-credentials_0005", null);
        insert("missing-secret", "repository_missing-secret_0006", new Document("type", "s3").append("auth_mode", "key_secret")
                .append("aws_access_key_id", "access"));
        insert("working", CLIENT, s3KeySecret());

        assertThat(single(provider.get()).opensearchProperties()).containsKey("s3.client.repository_archive-eu_7f3a.endpoint");
    }

    @Test
    void secretsEncryptedWithAnotherPasswordSecretAreSkipped() {
        // AES/CBC without integrity check may decrypt with a wrong key to garbage instead of failing, so a service
        // that can't decrypt stands in for a Data Node with another password_secret
        provider = provider(new EncryptedValueService("1234567890abcdef") {
            @Override
            public String decrypt(EncryptedValue value) {
                return null;
            }
        });
        insert("minio", CLIENT, s3KeySecret());

        assertThat(provider.get()).isEmpty();
    }

    private RepositoryClientsProvider provider(EncryptedValueService encryptedValueService) {
        return new RepositoryClientsProvider(mongoCollections, new RepositoryCredentialsMapper(encryptedValueService));
    }

    // Stores a record like the enterprise plugin of the server does
    private void insert(String repositoryName, String clientName, Document credentials) {
        records.insertOne(new Document("repository_name", repositoryName).append("client_name", clientName)
                .append("credentials", credentials));
    }

    private Document s3KeySecret() {
        return new Document("type", "s3").append("auth_mode", "key_secret")
                .append("aws_access_key_id", "access")
                .append("aws_secret_access_key", encrypted("secret"));
    }

    private Document encrypted(String value) {
        final EncryptedValue encrypted = encryptedValueService.encrypt(value);
        return new Document("encrypted_value", encrypted.value()).append("salt", encrypted.salt());
    }

    private static RepositoryConfiguration single(Collection<RepositoryConfiguration> clients) {
        assertThat(clients).hasSize(1);
        return clients.iterator().next();
    }
}

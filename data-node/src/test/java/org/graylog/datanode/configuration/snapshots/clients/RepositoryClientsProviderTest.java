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
import org.graylog2.datanode.repositories.RepositoryClientDto;
import org.graylog2.datanode.repositories.RepositoryClientService;
import org.graylog2.datanode.repositories.RepositoryClientSettings;
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
import static org.graylog2.shared.utilities.StringUtils.f;

@ExtendWith(MongoDBExtension.class)
class RepositoryClientsProviderTest {

    private final EncryptedValueService encryptedValueService = new EncryptedValueService("1234567890abcdef");
    private MongoCollections mongoCollections;
    private MongoCollection<Document> records;
    private RepositoryClientService clientService;
    private RepositoryClientsProvider provider;

    @BeforeEach
    void setUp(MongoCollections mongoCollections) {
        this.mongoCollections = mongoCollections;
        records = mongoCollections.nonEntityCollection(RepositoryClientDto.COLLECTION_NAME, Document.class);
        clientService = new RepositoryClientService(mongoCollections);
        provider = new RepositoryClientsProvider(mongoCollections, encryptedValueService);
    }

    @Test
    void noClients() {
        assertThat(provider.get()).isEmpty();
    }

    @Test
    void settingsGoToTheConfigurationAndSecureSettingsToTheKeystore() {
        final String client = minio("minio");

        final RepositoryConfiguration repository = single(provider.get());
        assertThat(repository.isRepositoryEnabled()).isTrue();
        assertThat(repository.opensearchProperties()).isEqualTo(Map.of(
                f("s3.client.%s.endpoint", client), "minio.local:9000",
                f("s3.client.%s.path_style_access", client), "true"));
        assertThat(repository.keystoreItems(null))
                .extracting("key", "secret")
                .containsExactlyInAnyOrder(
                        tuple(f("s3.client.%s.access_key", client), "access"),
                        tuple(f("s3.client.%s.secret_key", client), "secret"));
    }

    @Test
    void secureFilesGoToTheKeystoreAsFileContent() {
        final String client = clientService.create("gcs", name -> new RepositoryClientSettings(RepositoryClientSettings.TYPE_GCS,
                Map.of(), Map.of(), Map.of("credentials_file", encrypt("{\"type\": \"service_account\"}")))).clientName();

        assertThat(single(provider.get()).keystoreItems(null))
                .singleElement()
                .usingRecursiveComparison()
                .isEqualTo(new OpensearchKeystoreFileContentItem(f("gcs.client.%s.credentials_file", client),
                        "{\"type\": \"service_account\"}".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void secretsDoNotEndUpInTheDescriptionOfTheClient() {
        minio("minio");

        assertThat(single(provider.get()).toString()).contains("minio.local:9000").doesNotContain("=access", "=secret");
    }

    @Test
    void repositoriesGetTheirOwnClients() {
        final String first = minio("first");
        final String second = minio("second");

        assertThat(provider.get())
                .flatExtracting(repository -> repository.keystoreItems(null))
                .extracting("key")
                .contains(f("s3.client.%s.access_key", first), f("s3.client.%s.access_key", second));
    }

    @Test
    void secretsEncryptedWithAnotherPasswordSecretAreSkipped() {
        // AES/CBC without integrity check may decrypt with a wrong key to garbage instead of failing, so a service
        // that can't decrypt stands in for a Data Node with another password_secret
        provider = new RepositoryClientsProvider(mongoCollections, new EncryptedValueService("1234567890abcdef") {
            @Override
            public String decrypt(EncryptedValue value) {
                return null;
            }
        });
        minio("minio");

        assertThat(provider.get()).isEmpty();
    }

    @Test
    void clientNamesNotGeneratedByGraylogAreSkipped() {
        records.insertOne(new Document("repository_name", "dotted").append("client_name", "a.b")
                .append("opensearch", new Document("type", "s3")));
        final String client = minio("minio");

        assertThat(provider.get()).singleElement()
                .satisfies(repository -> assertThat(repository.opensearchProperties()).containsKey(f("s3.client.%s.endpoint", client)));
    }

    @Test
    void invalidRecordsDoNotAffectOthers() {
        records.insertOne(new Document("repository_name", "undecodable").append("client_name", "repository_undecodable_0001")
                .append("opensearch", "not-an-object"));
        records.insertOne(new Document("repository_name", "unknown-type").append("client_name", "repository_unknown-type_0001")
                .append("opensearch", new Document("type", "hdfs")));
        minio("minio");

        assertThat(provider.get()).hasSize(1);
    }

    @Test
    void failingDecryptionDoesNotAffectOthers() {
        final EncryptedValue failing = encrypt("failing");
        provider = new RepositoryClientsProvider(mongoCollections, new EncryptedValueService("1234567890abcdef") {
            @Override
            public String decrypt(EncryptedValue value) {
                if (value.value().equals(failing.value())) {
                    throw new IllegalStateException("decryption failed");
                }
                return super.decrypt(value);
            }
        });
        clientService.create("failing", name -> new RepositoryClientSettings(RepositoryClientSettings.TYPE_S3,
                Map.of(), Map.of("secret_key", failing), Map.of()));
        minio("working");

        assertThat(provider.get()).hasSize(1);
    }

    // Stores a client like the server does, returns the client name
    private String minio(String repositoryName) {
        return clientService.create(repositoryName, name -> s3("secret")).clientName();
    }

    private RepositoryClientSettings s3(String secretKey) {
        return new RepositoryClientSettings(RepositoryClientSettings.TYPE_S3,
                Map.of("endpoint", "minio.local:9000", "path_style_access", "true"),
                Map.of("access_key", encrypt("access"), "secret_key", encrypt(secretKey)),
                Map.of());
    }

    private EncryptedValue encrypt(String value) {
        return encryptedValueService.encrypt(value);
    }

    private static RepositoryConfiguration single(Collection<RepositoryConfiguration> repositories) {
        assertThat(repositories).hasSize(1);
        return repositories.iterator().next();
    }
}

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
package org.graylog2.datanode.repositories;

import com.mongodb.MongoException;
import org.bson.Document;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.MongoConnection;
import org.graylog2.security.encryption.EncryptedValue;
import org.graylog2.security.encryption.EncryptedValueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MongoDBExtension.class)
class RepositoryClientServiceTest {

    private static final String CLIENT = "repository_repo_7f3a";

    private final EncryptedValueService encryptedValueService = new EncryptedValueService("1234567890abcdef");
    private MongoCollections mongoCollections;
    private MongoConnection mongoConnection;
    private RepositoryClientService service;

    @BeforeEach
    void setUp(MongoCollections mongoCollections, MongoConnection mongoConnection) {
        this.mongoCollections = mongoCollections;
        this.mongoConnection = mongoConnection;
        service = new RepositoryClientService(mongoCollections);
    }

    @Test
    void createGeneratesTheClientNameAndPassesItToTheSettings() {
        final RepositoryClientDto client = service.create("repo", name -> s3(Map.of("role_session_name", name)));

        assertThat(RepositoryClientNames.isManaged(client.clientName())).isTrue();
        assertThat(client.clientName()).startsWith("repository_repo_");
        assertThat(client.opensearch().settings()).containsEntry("role_session_name", client.clientName());
        assertThat(service.get("repo")).contains(client);
    }

    @Test
    void createReplacesThePreviousClientOfTheRepository() {
        final String previous = service.create("repo", name -> s3(Map.of())).clientName();

        final String created = service.create("repo", name -> s3(Map.of())).clientName();

        assertThat(created).isNotEqualTo(previous);
        assertThat(service.get("repo")).hasValueSatisfying(client -> assertThat(client.clientName()).isEqualTo(created));
    }

    @Test
    void createRetriesWhenTheClientNameIsTaken() {
        service.save("other", CLIENT, s3(Map.of()));
        final Iterator<String> names = List.of(CLIENT, "repository_repo_0001").iterator();
        service = new RepositoryClientService(mongoCollections, repositoryName -> names.next());

        assertThat(service.create("repo", name -> s3(Map.of())).clientName()).isEqualTo("repository_repo_0001");
    }

    @Test
    void createGivesUpWhenNoClientNameIsFree() {
        service.save("other", CLIENT, s3(Map.of()));
        service = new RepositoryClientService(mongoCollections, repositoryName -> CLIENT);

        assertThatThrownBy(() -> service.create("repo", name -> s3(Map.of()))).isInstanceOf(MongoException.class);
        assertThat(service.get("repo")).isEmpty();
    }

    @Test
    void saveKeepsTheClientName() {
        service.save("repo", CLIENT, s3(Map.of("region", "eu-west-1")));
        service.save("repo", CLIENT, s3(Map.of("region", "eu-central-1")));

        assertThat(service.get("repo")).hasValueSatisfying(client -> {
            assertThat(client.clientName()).isEqualTo(CLIENT);
            assertThat(client.opensearch().settings()).isEqualTo(Map.of("region", "eu-central-1"));
        });
    }

    @Test
    void saveRejectsClientsNotManagedByGraylog() {
        assertThatThrownBy(() -> service.save("repo", "default", s3(Map.of())))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void secretsAreStoredEncrypted() {
        final EncryptedValue secret = encryptedValueService.encrypt("secret");
        service.save("repo", CLIENT, new RepositoryClientSettings(RepositoryClientSettings.TYPE_S3, Map.of(),
                Map.of("secret_key", secret), Map.of()));

        final Document stored = mongoConnection.getMongoDatabase().getCollection(RepositoryClientDto.COLLECTION_NAME).find().first();
        assertThat(stored).isNotNull();
        assertThat(stored.get("opensearch", Document.class).get("secure_settings", Document.class).get("secret_key", Document.class))
                .containsEntry("encrypted_value", secret.value())
                .containsEntry("salt", secret.salt());
        assertThat(service.get("repo")).hasValueSatisfying(client ->
                assertThat(encryptedValueService.decrypt(client.opensearch().secureSettings().get("secret_key"))).isEqualTo("secret"));
    }

    @Test
    void deleteIsIdempotent() {
        service.save("repo", CLIENT, s3(Map.of()));

        service.delete("repo");
        service.delete("repo");

        assertThat(service.get("repo")).isEmpty();
    }

    @Test
    void settingsOnlyAcceptKnownTypesAndPlainKeys() {
        assertThatThrownBy(() -> new RepositoryClientSettings("fs", Map.of(), Map.of(), Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> s3(Map.of("endpoint.other", "x")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(new RepositoryClientSettings(RepositoryClientSettings.TYPE_GCS, null, null, null).settings()).isEmpty();
    }

    private static RepositoryClientSettings s3(Map<String, String> settings) {
        return new RepositoryClientSettings(RepositoryClientSettings.TYPE_S3, settings, Map.of(), Map.of());
    }
}

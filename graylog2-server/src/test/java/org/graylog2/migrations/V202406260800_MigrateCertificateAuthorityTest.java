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
package org.graylog2.migrations;

import org.assertj.core.api.Assertions;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.cluster.certificates.EncryptedCaKeystore;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.HashSet;
import java.util.Set;

class V202406260800_MigrateCertificateAuthorityTest {

    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    @BeforeEach
    void setUp() {
        mongodb.start();
    }

    @AfterEach
    void tearDown() {
        mongodb.close();
    }

    @Test
    void testMigration() {
        mongodb.importFixture("V202406260800_MigrateCertificateAuthorityTest.json", V202406260800_MigrateCertificateAuthorityTest.class);
        final ClusterConfigService clusterConfigService = Mockito.mock(ClusterConfigService.class);
        final V202406260800_MigrateCertificateAuthority migration = new V202406260800_MigrateCertificateAuthority(clusterConfigService, mongodb.mongoConnection());


        final long documentsCount = mongodb.mongoConnection().getMongoDatabase().getCollection(V202406260800_MigrateCertificateAuthority.LEGACY_COLLECTION_NAME).countDocuments();
        // there should be one entry with the encoded CA keystore
        Assertions.assertThat(documentsCount).isEqualTo(1);

        migration.upgrade();
        final ArgumentCaptor<EncryptedCaKeystore> captor = ArgumentCaptor.forClass(EncryptedCaKeystore.class);
        Mockito.verify(clusterConfigService, Mockito.times(1)).write(captor.capture());

        // verify that migration extracted the correct data
        Assertions.assertThat(captor.getValue().keystore().value()).startsWith("b97f382e52e0cf");
        Assertions.assertThat(captor.getValue().keystore().salt()).isEqualTo("753bacc1ae1df5e3");

        final Set<String> existingCollections = mongodb.mongoConnection().getMongoDatabase().listCollectionNames().into(new HashSet<>());
        Assertions.assertThat(existingCollections).doesNotContain(V202406260800_MigrateCertificateAuthority.LEGACY_COLLECTION_NAME);
    }
}

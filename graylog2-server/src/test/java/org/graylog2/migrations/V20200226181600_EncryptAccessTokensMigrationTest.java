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

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.Configuration;
import org.graylog2.security.AccessTokenCipher;
import org.graylog2.security.AccessTokenImpl;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.security.AccessTokenImpl.COLLECTION_NAME;
import static org.graylog2.security.AccessTokenImpl.LAST_ACCESS;
import static org.graylog2.security.AccessTokenImpl.NAME;
import static org.graylog2.security.AccessTokenImpl.TOKEN;
import static org.graylog2.security.AccessTokenImpl.TOKEN_TYPE;
import static org.graylog2.security.AccessTokenImpl.USERNAME;
import static org.mockito.Mockito.when;

public class V20200226181600_EncryptAccessTokensMigrationTest {
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    public Configuration configuration;

    private V20200226181600_EncryptAccessTokensMigration migration;
    private MongoCollection<Document> collection;

    @Before
    public void setUp() throws Exception {
        when(configuration.getPasswordSecret()).thenReturn("Q53B8mmRGAB9f2Jwuo6CPzvU5gheJWq8vVPmU7E7JS8vBtxbAxVWHk5S0thQDu2Xu6jTELyNqiHNc6MMY7kYtziaIMEenImp");

        migration = new V20200226181600_EncryptAccessTokensMigration(new AccessTokenCipher(configuration), mongodb.mongoConnection());
        collection = mongodb.mongoConnection().getMongoDatabase().getCollection(COLLECTION_NAME);
    }

    @Test
    @MongoDBFixtures("V20200226181600_EncryptAccessTokensMigrationTest.json")
    public void upgrade() {
        final Document plainToken1 = collection.find(Filters.eq("_id", new ObjectId("54e3deadbeefdeadbeef0001"))).first();
        final Document plainToken2 = collection.find(Filters.eq("_id", new ObjectId("54e3deadbeefdeadbeef0002"))).first();
        final Document plainToken3 = collection.find(Filters.eq("_id", new ObjectId("54e3deadbeefdeadbeef0003"))).first();

        assertThat(plainToken1).isNotNull();
        assertThat(plainToken2).isNotNull();
        assertThat(plainToken3).isNotNull();

        migration.upgrade();

        final Document encryptedToken1 = collection.find(Filters.eq("_id", new ObjectId("54e3deadbeefdeadbeef0001"))).first();
        final Document encryptedToken2 = collection.find(Filters.eq("_id", new ObjectId("54e3deadbeefdeadbeef0002"))).first();
        final Document encryptedToken3 = collection.find(Filters.eq("_id", new ObjectId("54e3deadbeefdeadbeef0003"))).first();

        assertThat(plainToken1).isNotEqualTo(encryptedToken1); // Must be encrypted, so not equal
        assertThat(plainToken2).isEqualTo(encryptedToken2);    // Already was encrypted, migration shouldn't touch it
        assertThat(plainToken3).isNotEqualTo(encryptedToken3); // Must be encrypted, so not equal

        // Newly encrypted token
        assertThat(encryptedToken1).satisfies(t -> {
            final Document token = (Document) t;

            assertThat(token.getString(NAME)).isEqualTo("cli-access");
            assertThat(token.getString(USERNAME)).isEqualTo("jane");
            assertThat(token.getString(TOKEN)).isEqualTo("cc21d0e8fcbf8c28f8fd56c30e81e5f92cf8bcbf846c69cdcc4eec5ffe64f592bf604141be77e46c819a8997d9d245f1bc9f5f60dc44e490ca6ad07b25d45338efb3bad5");
            assertThat(token.getInteger(TOKEN_TYPE)).isEqualTo(AccessTokenImpl.Type.AES_SIV.getIntValue());
            assertThat(token.get(LAST_ACCESS)).isEqualTo(DateTime.parse("2020-02-26T21:50:12.454Z").toDate());
        });

        // Already encrypted token
        assertThat(encryptedToken2).satisfies(t -> {
            final Document token = (Document) t;

            assertThat(token.getString(NAME)).isEqualTo("test-1");
            assertThat(token.getString(USERNAME)).isEqualTo("john");
            assertThat(token.getString(TOKEN)).isEqualTo("d5f6fc27206946c15f183764c32526674108f9f4");
            assertThat(token.getInteger(TOKEN_TYPE)).isEqualTo(AccessTokenImpl.Type.AES_SIV.getIntValue());
            assertThat(token.get(LAST_ACCESS)).isEqualTo(DateTime.parse("2020-01-27T16:23:02.758Z").toDate());
        });

        // Newly encrypted token
        assertThat(encryptedToken3).satisfies(t -> {
            final Document token = (Document) t;

            assertThat(token.getString(NAME)).isEqualTo("test-2");
            assertThat(token.getString(USERNAME)).isEqualTo("john");
            assertThat(token.getString(TOKEN)).isEqualTo("55acb1b25c787ae1bc91e7eb1694b39277881b4e4643369590c49afd18ac745b4b60ad4aab058dfb4b6f00eba8d30a2c4c188cc9b5832cd9d9620aab82281651797ff3");
            assertThat(token.getInteger(TOKEN_TYPE)).isEqualTo(AccessTokenImpl.Type.AES_SIV.getIntValue());
            assertThat(token.get(LAST_ACCESS)).isEqualTo(DateTime.parse("2020-01-27T18:23:02.758Z").toDate());
        });
    }
}

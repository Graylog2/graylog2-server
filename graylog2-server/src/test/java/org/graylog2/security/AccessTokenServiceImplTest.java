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
package org.graylog2.security;

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.users.UserConfiguration;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class AccessTokenServiceImplTest {
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private PaginatedAccessTokenEntityService paginatedAccessTokenEntityService;

    @Mock
    private ClusterConfigService configService;

    private AccessTokenService accessTokenService;

    @Before
    public void setupService() {
        // Simple cipher which reverses the cleartext. DB fixtures need to contain the reversed (i.e. encrypted token)
        final AccessTokenCipher accessTokenCipher = mock(AccessTokenCipher.class);
        when(accessTokenCipher.encrypt(anyString())).then(inv -> StringUtils.reverse(inv.getArgument(0)));
        when(accessTokenCipher.decrypt(anyString())).then(inv -> StringUtils.reverse(inv.getArgument(0)));

        this.accessTokenService = new AccessTokenServiceImpl(mongodb.mongoConnection(), paginatedAccessTokenEntityService, accessTokenCipher, configService);
    }

    @After
    public void tearDown() {
        mongodb.mongoConnection().getMongoDatabase().drop();
    }

    @Test
    public void testLoadNoToken() {
        final AccessToken accessToken = accessTokenService.load("foobar");
        assertNull("No token should have been returned", accessToken);
        verifyNoMoreInteractions(paginatedAccessTokenEntityService, configService);
    }

    @Test
    @MongoDBFixtures("accessTokensSingleToken.json")
    public void testLoadSingleToken() {
        final AccessToken accessToken = accessTokenService.load("foobar");
        assertNotNull("Matching token should have been returned", accessToken);
        assertEquals("foobar", accessToken.getToken());
        assertEquals("web", accessToken.getName());
        assertEquals("admin", accessToken.getUserName());
        assertEquals(DateTime.parse("2015-03-14T15:09:26.540Z"), accessToken.getLastAccess());
        verifyNoMoreInteractions(paginatedAccessTokenEntityService, configService);
    }

    @Test
    @MongoDBFixtures("accessTokensMultipleTokens.json")
    public void testLoadAll() {
        final List<AccessToken> tokens = accessTokenService.loadAll("admin");

        assertNotNull("Should have returned token list", tokens);
        assertEquals(2, tokens.size());
        verifyNoMoreInteractions(paginatedAccessTokenEntityService, configService);
    }

    @Test
    @MongoDBFixtures("accessTokensMultipleTokens.json")
    public void testLoadById() {
        assertThat(accessTokenService.loadById("54e3deadbeefdeadbeefaffe"))
                .isNotNull()
                .satisfies(token -> {
                    assertThat(token.getId()).isEqualTo("54e3deadbeefdeadbeefaffe");
                    assertThat(token.getName()).isEqualTo("web");
                });

        assertThat(accessTokenService.loadById("54f9deadbeefdeadbeefaffe"))
                .isNotNull()
                .satisfies(token -> {
                    assertThat(token.getId()).isEqualTo("54f9deadbeefdeadbeefaffe");
                    assertThat(token.getName()).isEqualTo("rest");
                });

        assertThat(accessTokenService.loadById("54f9deadbeefdeadbeef0000"))
                .as("check that loading a non-existent token returns null")
                .isNull();
        verifyNoMoreInteractions(paginatedAccessTokenEntityService, configService);
    }

    @Test
    public void testCreate() {
        final String username = "admin";
        final String tokenname = "web";
        final int ttlInDays = 30;

        assertEquals(0, accessTokenService.loadAll(username).size());
        final AccessToken token = accessTokenService.create(username, tokenname, Duration.ofDays(ttlInDays));

        assertEquals(1, accessTokenService.loadAll(username).size());
        assertNotNull("Should have returned token", token);
        assertEquals("Username before and after saving should be equal", username, token.getUserName());
        assertEquals("Token before and after saving should be equal", tokenname, token.getName());
        assertNotNull("Token should not be null", token.getToken());
        assertNotNull("\"CreatedAt\" should not be null", token.getCreatedAt());
        assertNotNull("\"ExpiresAt\" should not be null", token.getExpiresAt());
        assertEquals("Expiration-timestamp should be " + ttlInDays + " days after creation-timestamp", token.getCreatedAt().plusDays(ttlInDays), token.getExpiresAt());
        verifyNoMoreInteractions(paginatedAccessTokenEntityService, configService);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testCreateWithoutTtl() {
        final String username = "admin";
        final String tokenname = "web";

        assertEquals(0, accessTokenService.loadAll(username).size());
        when(configService.getOrDefault(UserConfiguration.class, UserConfiguration.DEFAULT_VALUES)).thenReturn(UserConfiguration.DEFAULT_VALUES);
        final AccessToken token = accessTokenService.create(username, tokenname);

        assertEquals(1, accessTokenService.loadAll(username).size());
        assertNotNull("Should have returned token", token);
        assertEquals("Username before and after saving should be equal", username, token.getUserName());
        assertEquals("Token before and after saving should be equal", tokenname, token.getName());
        assertNotNull("Token should not be null", token.getToken());

        verify(configService).getOrDefault(UserConfiguration.class, UserConfiguration.DEFAULT_VALUES);
        verifyNoMoreInteractions(paginatedAccessTokenEntityService, configService);
    }

    @Test
    public void testCreateEncryptsToken() {
        final String username = "jane";
        final String tokenName = "very-secret";

        assertThat(accessTokenService.loadAll(username)).isEmpty();

        final AccessToken token = accessTokenService.create(username, tokenName, Duration.ofDays(30));

        assertThat(accessTokenService.loadAll(username)).hasSize(1);

        assertThat(token.getUserName()).isEqualTo(username);
        assertThat(token.getName()).isEqualTo(tokenName);
        assertThat(token.getLastAccess()).isEqualTo(new DateTime(0, DateTimeZone.UTC));
        assertThat(token.getToken()).isNotBlank();

        // Need to access the collection on a lower level to get access to the encrypted token and the token_type
        final MongoCollection<Document> collection = mongodb.mongoConnection().getMongoDatabase().getCollection("access_tokens");

        assertThat(collection.find(Filters.eq("_id", new ObjectId(token.getId()))).first())
                .as("check that token %s/%s exists", token.getId(), token.getName())
                .isNotNull()
                .satisfies(t -> {
                    final Document tokenDocument = (Document) t;

                    assertThat(tokenDocument.getString("token"))
                            .as("check for token %s/%s to be encrypted", token.getId(), token.getName())
                            .isEqualTo(StringUtils.reverse(token.getToken()));
                    assertThat(tokenDocument.getInteger("token_type"))
                            .as("check that token type is set for %s/%s", token.getId(), token.getName())
                            .isEqualTo(AccessTokenImpl.Type.AES_SIV.getIntValue());
                });
        verifyNoMoreInteractions(paginatedAccessTokenEntityService, configService);
    }

    @Test
    @MongoDBFixtures("accessTokensSingleToken.json")
    public void testTouch() throws Exception {
        accessTokenService.setLastAccessCache(1, TimeUnit.NANOSECONDS);
        final AccessToken token = accessTokenService.load("foobar");
        final DateTime firstAccess = accessTokenService.touch(token);

        Thread.sleep(1,0);
        final DateTime secondAccess = accessTokenService.touch(token);
        assertThat(secondAccess).isGreaterThan(firstAccess);
        verifyNoMoreInteractions(paginatedAccessTokenEntityService, configService);
    }

    @Test
    public void testSave() throws Exception {
        final String username = "admin";
        final String tokenname = "web";
        final String tokenString = "foobar";

        assertNull(accessTokenService.load(tokenString));
        assertEquals(0, accessTokenService.loadAll(username).size());

        final AccessToken token = accessTokenService.create(username, tokenname, Duration.ofDays(30));
        token.setToken(tokenString);

        accessTokenService.save(token);

        assertEquals(1, accessTokenService.loadAll(username).size());
        final AccessToken newToken = accessTokenService.load(tokenString);

        assertNotNull(newToken);
        assertEquals(token.getUserName(), newToken.getUserName());
        assertEquals(token.getName(), newToken.getName());
        assertEquals(token.getToken(), newToken.getToken());
        verifyNoMoreInteractions(paginatedAccessTokenEntityService, configService);
    }

    @Test
    @MongoDBFixtures("accessTokensSingleToken.json")
    public void testExceptionForMultipleTokens() {
        final AccessToken existingToken = accessTokenService.load("foobar");
        final AccessToken newToken = accessTokenService.create("user", "foobar", Duration.ofDays(30));
        newToken.setToken(existingToken.getToken());
        assertThatThrownBy(() -> accessTokenService.save(newToken))
                .isInstanceOfSatisfying(MongoException.class, e ->
                        assertThat(MongoUtils.isDuplicateKeyError(e)).isTrue()
                );
        verifyNoMoreInteractions(paginatedAccessTokenEntityService, configService);
    }
}

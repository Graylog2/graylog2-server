/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.security;

import org.apache.commons.lang3.StringUtils;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.security.token.AccessTokenCipher;
import org.graylog2.security.token.AccessTokenServiceImpl;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static org.assertj.jodatime.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AccessTokenServiceImplTest {
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    private AccessTokenService accessTokenService;

    @Before
    public void setupService () {
        // Simple cipher which reverses the cleartext. DB fixtures need to contain the reversed (i.e. encrypted token)
        final AccessTokenCipher accessTokenCipher = mock(AccessTokenCipher.class);
        when(accessTokenCipher.encrypt(anyString())).then(inv -> StringUtils.reverse(inv.getArgument(0)));
        when(accessTokenCipher.decrypt(anyString())).then(inv -> StringUtils.reverse(inv.getArgument(0)));

        this.accessTokenService = new AccessTokenServiceImpl(mongodb.mongoConnection(), accessTokenCipher);
    }

    @After
    public void tearDown() {
        mongodb.mongoConnection().getMongoDatabase().drop();
    }

    @Test
    public void testLoadNoToken() throws Exception {
        final AccessToken accessToken = accessTokenService.load("foobar");
        assertNull("No token should have been returned", accessToken);
    }

    @Test
    @MongoDBFixtures("accessTokensSingleToken.json")
    public void testLoadSingleToken() throws Exception {
        final AccessToken accessToken = accessTokenService.load("foobar");
        assertNotNull("Matching token should have been returned", accessToken);
        assertEquals("foobar", accessToken.getToken());
        assertEquals("web", accessToken.getName());
        assertEquals("admin", accessToken.getUserName());
        assertEquals(DateTime.parse("2015-03-14T15:09:26.540Z"), accessToken.getLastAccess());
    }

    @Test
    @MongoDBFixtures("accessTokensMultipleTokens.json")
    public void testLoadAll() throws Exception {
        final List<AccessToken> tokens = accessTokenService.loadAll("admin");

        assertNotNull("Should have returned token list", tokens);
        assertEquals(2, tokens.size());
    }

    @Test
    public void testCreate() throws Exception {
        final String username = "admin";
        final String tokenname = "web";

        assertEquals(0, accessTokenService.loadAll(username).size());
        final AccessToken token = accessTokenService.create(username, tokenname);

        assertEquals(1, accessTokenService.loadAll(username).size());
        assertNotNull("Should have returned token", token);
        assertEquals("Username before and after saving should be equal", username, token.getUserName());
        assertEquals("Token before and after saving should be equal", tokenname, token.getName());
        assertNotNull("Token should not be null", token.getToken());
    }

    @Test
    @MongoDBFixtures("accessTokensSingleToken.json")
    public void testTouch() throws Exception {
        final AccessToken token = accessTokenService.load("foobar");
        final DateTime initialLastAccess = token.getLastAccess();

        accessTokenService.touch(token);

        assertThat(token.getLastAccess()).isAfter(initialLastAccess);
    }

    @Test
    public void testSave() throws Exception {
        final String username = "admin";
        final String tokenname = "web";
        final String tokenString = "foobar";

        assertNull(accessTokenService.load(tokenString));
        assertEquals(0, accessTokenService.loadAll(username).size());

        final AccessToken token = accessTokenService.create(username, tokenname);
        token.setToken(tokenString);

        accessTokenService.save(token);

        assertEquals(1, accessTokenService.loadAll(username).size());
        final AccessToken newToken = accessTokenService.load(tokenString);

        assertNotNull(newToken);
        assertEquals(token.getUserName(), newToken.getUserName());
        assertEquals(token.getName(), newToken.getName());
        assertEquals(token.getToken(), newToken.getToken());
    }

    @Test(expected = IllegalStateException.class)
    @MongoDBFixtures("accessTokensMultipleIdenticalTokens.json")
    public void testExceptionForMultipleTokens() throws Exception {
        accessTokenService.load("foobar");
    }
}

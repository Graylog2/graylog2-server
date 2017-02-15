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

import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import com.lordofthejars.nosqlunit.core.LoadStrategyEnum;
import com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb;
import org.graylog2.database.MongoConnectionRule;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb.InMemoryMongoRuleBuilder.newInMemoryMongoDbRule;
import static org.assertj.jodatime.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class AccessTokenServiceImplTest {
    @ClassRule
    public static final InMemoryMongoDb IN_MEMORY_MONGO_DB = newInMemoryMongoDbRule().build();

    @Rule
    public MongoConnectionRule mongoRule = MongoConnectionRule.build("test");

    private AccessTokenService accessTokenService;

    @Before
    public void setupService () {
        this.accessTokenService = new AccessTokenServiceImpl(mongoRule.getMongoConnection());
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void testLoadNoToken() throws Exception {
        final AccessToken accessToken = accessTokenService.load("foobar");
        assertNull("No token should have been returned", accessToken);
    }

    @Test
    @UsingDataSet(locations = "accessTokensSingleToken.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void testLoadSingleToken() throws Exception {
        final AccessToken accessToken = accessTokenService.load("foobar");
        assertNotNull("Matching token should have been returned", accessToken);
        assertEquals("foobar", accessToken.getToken());
        assertEquals("web", accessToken.getName());
        assertEquals("admin", accessToken.getUserName());
        assertEquals(DateTime.parse("2015-03-14T15:09:26.540Z"), accessToken.getLastAccess());
    }

    @Test
    @UsingDataSet(locations = "accessTokensMultipleTokens.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void testLoadAll() throws Exception {
        final List<AccessToken> tokens = accessTokenService.loadAll("admin");

        assertNotNull("Should have returned token list", tokens);
        assertEquals(2, tokens.size());
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
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
    @UsingDataSet(locations = "accessTokensSingleToken.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void testTouch() throws Exception {
        final AccessToken token = accessTokenService.load("foobar");
        final DateTime initialLastAccess = token.getLastAccess();

        accessTokenService.touch(token);

        assertThat(token.getLastAccess()).isAfter(initialLastAccess);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
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
    @UsingDataSet(locations = "accessTokensMultipleIdenticalTokens.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void testExceptionForMultipleTokens() throws Exception {
        accessTokenService.load("foobar");
    }
}
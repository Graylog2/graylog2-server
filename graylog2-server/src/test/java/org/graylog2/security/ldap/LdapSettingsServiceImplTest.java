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
package org.graylog2.security.ldap;

import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import com.lordofthejars.nosqlunit.core.LoadStrategyEnum;
import com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import org.bson.types.ObjectId;
import org.graylog2.Configuration;
import org.graylog2.database.MongoConnectionRule;
import org.graylog2.shared.security.ldap.LdapSettings;
import org.graylog2.users.RoleService;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.net.URI;
import java.util.Map;

import static com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb.InMemoryMongoRuleBuilder.newInMemoryMongoDbRule;
import static org.assertj.core.api.Assertions.assertThat;

public class LdapSettingsServiceImplTest {
    @ClassRule
    public static final InMemoryMongoDb IN_MEMORY_MONGO_DB = newInMemoryMongoDbRule().build();

    @Rule
    public final MongoConnectionRule mongoRule = MongoConnectionRule.build("test");

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private final Configuration configuration = new Configuration() {
        @Override
        public String getPasswordSecret() {
            return "asdfasdfasdfasdfasdf";
        }
    };
    private final LdapSettingsImpl.Factory factory = new LdapSettingsImpl.Factory() {
        @Override
        public LdapSettingsImpl createEmpty() {
            return new LdapSettingsImpl(configuration, roleService);
        }

        @Override
        public LdapSettingsImpl create(ObjectId objectId, Map<String, Object> fields) {
            return new LdapSettingsImpl(configuration, roleService, objectId, fields);
        }
    };

    @Mock
    private RoleService roleService;
    private LdapSettingsServiceImpl ldapSettingsService;

    @Before
    public void setUp() throws Exception {
        ldapSettingsService = new LdapSettingsServiceImpl(mongoRule.getMongoConnection(), factory);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void loadReturnsLdapSettings() throws Exception {
        final LdapSettings ldapSettings = ldapSettingsService.load();
        assertThat(ldapSettings).isNotNull();
        assertThat(ldapSettings.getId()).isEqualTo("54e3deadbeefdeadbeefaffe");
        assertThat(ldapSettings.getUri()).isEqualTo(URI.create("ldap://localhost:389"));
        assertThat(ldapSettings.getSystemPassword()).isEqualTo("password");
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT, locations = "LdapSettingsServiceImplTest-invalid-password.json")
    public void loadReturnNullIfPasswordSecretIsWrong() throws Exception {
        final LdapSettings ldapSettings = ldapSettingsService.load();
        assertThat(ldapSettings).isNull();
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void loadReturnsNullIfDatabaseIsEmpty() throws Exception {
        assertThat(ldapSettingsService.load()).isNull();
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void loadReturnsNullIfDatabaseHasMoreThanOneEntry() throws Exception {
        final DBCollection collection = mongoRule.getMongoConnection().getDatabase().getCollection("ldap_settings");
        collection.insert(new BasicDBObject("foo", "bar"), new BasicDBObject("quux", "baz"));
        assertThat(ldapSettingsService.load()).isNull();
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void deleteRemovesAllLdapSettings() throws Exception {
        final DBCollection collection = mongoRule.getMongoConnection().getDatabase().getCollection("ldap_settings");
        assertThat(collection.count()).isEqualTo(1L);
        ldapSettingsService.delete();
        assertThat(collection.count()).isEqualTo(0);
    }
}

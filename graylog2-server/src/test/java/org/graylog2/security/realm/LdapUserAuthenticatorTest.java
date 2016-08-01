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
package org.graylog2.security.realm;

import com.google.common.collect.Maps;
import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import com.lordofthejars.nosqlunit.core.LoadStrategyEnum;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifFiles;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.annotations.LoadSchema;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.partition.impl.avl.AvlPartition;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.graylog2.Configuration;
import org.graylog2.plugin.database.users.User;
import org.graylog2.security.ldap.LdapConnector;
import org.graylog2.security.ldap.LdapSettingsImpl;
import org.graylog2.security.ldap.LdapSettingsService;
import org.graylog2.shared.security.Permissions;
import org.graylog2.shared.security.ldap.LdapEntry;
import org.graylog2.shared.security.ldap.LdapSettings;
import org.graylog2.shared.users.UserService;
import org.graylog2.users.RoleService;
import org.graylog2.users.UserImpl;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@RunWith(FrameworkRunner.class)
@CreateLdapServer(transports = {
        @CreateTransport(protocol = "LDAP")
})
@CreateDS(
        name = "LdapUserAuthenticatorTest",
        partitions = {
                @CreatePartition(
                        name = "example.com",
                        type = AvlPartition.class,
                        suffix = "dc=example,dc=com",
                        contextEntry = @ContextEntry(
                                entryLdif = "dn: dc=example,dc=com\n" +
                                        "dc: example\n" +
                                        "objectClass: top\n" +
                                        "objectClass: domain\n\n"

                        ),
                        indexes = {
                                @CreateIndex(attribute = "objectClass"),
                                @CreateIndex(attribute = "dc"),
                                @CreateIndex(attribute = "ou")
                        }

                )
        },
        loadedSchemas = {
                @LoadSchema(name = "nis", enabled = true)
        }
)
@ApplyLdifFiles("org/graylog2/security/ldap/base.ldif")
public class LdapUserAuthenticatorTest extends AbstractLdapTestUnit {
    private static final String ADMIN_DN = "uid=admin,ou=system";
    private static final String ADMIN_PASSWORD = "secret";

    private static final AuthenticationToken VALID_TOKEN = new UsernamePasswordToken("john", "test");
    private static final AuthenticationToken INVALID_TOKEN = new UsernamePasswordToken("john", "__invalid__");
    private static final String PASSWORD_SECRET = "r8Om85b0zgHmiGsK86T3ZFlmSIdMd3hcKmOa4T60MSPEobfRCTLNOK4T91GdHbGx";

    private LdapConnector ldapConnector;
    private LdapServer server;
    private LdapSettingsService ldapSettingsService;
    private LdapSettings ldapSettings;
    private Configuration configuration;
    private UserService userService;

    @Before
    public void setUp() throws Exception {
        server = getLdapServer();
        final LdapConnectionConfig ldapConfig = new LdapConnectionConfig();

        ldapConfig.setLdapHost("localHost");
        ldapConfig.setLdapPort(server.getPort());
        ldapConfig.setName(ADMIN_DN);
        ldapConfig.setCredentials(ADMIN_PASSWORD);

        configuration = mock(Configuration.class);
        when(configuration.getPasswordSecret()).thenReturn(PASSWORD_SECRET);

        ldapConnector = new LdapConnector(10000);
        ldapSettingsService = mock(LdapSettingsService.class);
        userService = mock(UserService.class);

        ldapSettings = new LdapSettingsImpl(configuration, mock(RoleService.class));
        ldapSettings.setEnabled(true);
        ldapSettings.setUri(URI.create("ldap://localhost:" + server.getPort()));
        ldapSettings.setUseStartTls(false);
        ldapSettings.setSystemUsername(ADMIN_DN);
        ldapSettings.setSystemPassword(ADMIN_PASSWORD);
        ldapSettings.setSearchBase("ou=users,dc=example,dc=com");
        ldapSettings.setSearchPattern("(&(objectClass=posixAccount)(uid={0}))");
        ldapSettings.setDisplayNameAttribute("cn");
        ldapSettings.setActiveDirectory(false);
        ldapSettings.setGroupSearchBase("ou=groups,dc=example,dc=com");
        ldapSettings.setGroupIdAttribute("cn");
        ldapSettings.setGroupSearchPattern("(|(objectClass=groupOfNames)(objectClass=posixGroup))");
    }

    @Test
    public void testDoGetAuthenticationInfo() throws Exception {
        final LdapUserAuthenticator authenticator = spy(new LdapUserAuthenticator(ldapConnector,
                                                                              ldapSettingsService,
                                                                              userService,
                                                                              mock(RoleService.class),
                                                                              DateTimeZone.UTC));

        when(ldapSettingsService.load())
                .thenReturn(ldapSettings);
        doReturn(mock(User.class))
                .when(authenticator)
                .syncFromLdapEntry(any(LdapEntry.class),any(LdapSettings.class), anyString());

        assertThat(authenticator.doGetAuthenticationInfo(VALID_TOKEN)).isNotNull();
        assertThat(authenticator.doGetAuthenticationInfo(INVALID_TOKEN)).isNull();
    }

    @Test
    public void testDoGetAuthenticationInfoDeniesEmptyPassword() throws Exception {
        final LdapUserAuthenticator authenticator = new LdapUserAuthenticator(ldapConnector,
                                                                              ldapSettingsService,
                                                                              userService,
                                                                              mock(RoleService.class),
                                                                              DateTimeZone.UTC);

        when(ldapSettingsService.load()).thenReturn(ldapSettings);

        assertThat(authenticator.doGetAuthenticationInfo(new UsernamePasswordToken("john", (char[]) null))).isNull();
        assertThat(authenticator.doGetAuthenticationInfo(new UsernamePasswordToken("john", new char[0]))).isNull();
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void testSyncFromLdapEntry() {
        final LdapUserAuthenticator authenticator = spy(new LdapUserAuthenticator(ldapConnector,
                                                                                  ldapSettingsService,
                                                                                  userService,
                                                                                  mock(RoleService.class),
                                                                                  DateTimeZone.UTC));

        final LdapEntry userEntry = new LdapEntry();
        final LdapSettings ldapSettings = mock(LdapSettings.class);
        when(ldapSettings.getDisplayNameAttribute()).thenReturn("displayName");
        when(ldapSettings.getDefaultGroupId()).thenReturn("54e3deadbeefdeadbeef0001");
        when(ldapSettings.getAdditionalDefaultGroupIds()).thenReturn(Collections.emptySet());

        when(userService.create())
                .thenReturn(new UserImpl(null, new Permissions(Collections.emptySet()), Maps.newHashMap()));

        final User ldapUser = authenticator.syncFromLdapEntry(userEntry, ldapSettings, "user");

        assertThat(ldapUser).isNotNull();
        assertThat(ldapUser.isExternalUser()).isTrue();
        assertThat(ldapUser.getName()).isEqualTo("user");
        assertThat(ldapUser.getEmail()).isEqualTo("user@localhost");
        assertThat(ldapUser.getHashedPassword()).isEqualTo("User synced from LDAP.");
        assertThat(ldapUser.getTimeZone()).isEqualTo(DateTimeZone.UTC);
        assertThat(ldapUser.getRoleIds()).containsOnly("54e3deadbeefdeadbeef0001");
        assertThat(ldapUser.getPermissions()).isNotEmpty();
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void testSyncFromLdapEntryExistingUser() {
        final LdapUserAuthenticator authenticator = spy(new LdapUserAuthenticator(ldapConnector,
                                                                                  ldapSettingsService,
                                                                                  userService,
                                                                                  mock(RoleService.class),
                                                                                  DateTimeZone.UTC));

        final LdapEntry userEntry = new LdapEntry();
        final LdapSettings ldapSettings = mock(LdapSettings.class);
        when(ldapSettings.getDisplayNameAttribute()).thenReturn("displayName");
        when(ldapSettings.getDefaultGroupId()).thenReturn("54e3deadbeefdeadbeef0001");
        when(ldapSettings.getAdditionalDefaultGroupIds()).thenReturn(Collections.emptySet());

        final HashMap<String, Object> fields = Maps.newHashMap();
        fields.put("permissions", Collections.singletonList("test:permission:1234"));
        when(userService.load(anyString()))
                .thenReturn(new UserImpl(null, new Permissions(Collections.emptySet()), fields));

        final User ldapUser = authenticator.syncFromLdapEntry(userEntry, ldapSettings, "user");

        assertThat(ldapUser).isNotNull();
        assertThat(ldapUser.getPermissions()).contains("test:permission:1234");
        assertThat(ldapUser.isExternalUser()).isTrue();
        assertThat(ldapUser.getName()).isEqualTo("user");
        assertThat(ldapUser.getEmail()).isEqualTo("user@localhost");
        assertThat(ldapUser.getHashedPassword()).isEqualTo("User synced from LDAP.");
        assertThat(ldapUser.getTimeZone()).isEqualTo(DateTimeZone.UTC);
        assertThat(ldapUser.getRoleIds()).containsOnly("54e3deadbeefdeadbeef0001");
        assertThat(ldapUser.getPermissions()).isNotEmpty();
    }
}

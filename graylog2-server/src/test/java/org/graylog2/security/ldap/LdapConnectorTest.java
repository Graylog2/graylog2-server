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

import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
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
import org.graylog2.shared.security.ldap.LdapEntry;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(FrameworkRunner.class)
@CreateLdapServer(transports = {
        @CreateTransport(protocol = "LDAP")
})
@CreateDS(
        name = "LdapConnectorTest",
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
public class LdapConnectorTest extends AbstractLdapTestUnit {
    private static final String ADMIN_DN = "uid=admin,ou=system";
    private static final String ADMIN_PASSWORD = "secret";

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    private LdapConnector connector;
    private LdapNetworkConnection connection;

    @Before
    public void setUp() throws Exception {
        final LdapServer server = getLdapServer();
        final LdapConnectionConfig config = new LdapConnectionConfig();

        config.setLdapHost("localHost");
        config.setLdapPort(server.getPort());
        config.setName(ADMIN_DN);
        config.setCredentials(ADMIN_PASSWORD);

        connector = new LdapConnector(10000);
        connection = connector.connect(config);
    }

    @After
    public void tearDown() throws Exception {
        connection.close();
    }

    @Test
    public void testUserLookup() throws Exception {
        final LdapEntry entry = connector.search(connection,
                "ou=users,dc=example,dc=com",
                "(&(objectClass=posixAccount)(uid={0}))",
                "cn",
                "john",
                false,
                "ou=groups,dc=example,dc=com",
                "cn",
                "(|(objectClass=groupOfNames)(objectClass=posixGroup))");

        assertThat(entry).isNotNull();
        assertThat(entry.getDn())
                .isNotNull()
                .isEqualTo("cn=John Doe,ou=users,dc=example,dc=com");

        assertThat(entry.getGroups())
                .hasSize(2)
                .contains("QA", "Developers");
    }

    @Test
    public void testGroupOfNamesLookup() throws Exception {
        final LdapEntry entry = connector.search(connection,
                "ou=users,dc=example,dc=com",
                "(&(objectClass=posixAccount)(uid={0}))",
                "cn",
                "john",
                false,
                "ou=groups,dc=example,dc=com",
                "cn",
                "(objectClass=groupOfNames)");

        assertThat(entry).isNotNull();
        assertThat(entry.getDn())
                .isNotNull()
                .isEqualTo("cn=John Doe,ou=users,dc=example,dc=com");

        assertThat(entry.getGroups()).hasSize(1).contains("QA");
    }

    @Test
    public void testGroupOfUniqueNamesLookup() throws Exception {
        final LdapEntry entry = connector.search(connection,
                "ou=users,dc=example,dc=com",
                "(&(objectClass=posixAccount)(uid={0}))",
                "cn",
                "john",
                false,
                "ou=groups,dc=example,dc=com",
                "cn",
                "(objectClass=groupOfUniqueNames)");

        assertThat(entry).isNotNull();
        assertThat(entry.getDn())
                .isNotNull()
                .isEqualTo("cn=John Doe,ou=users,dc=example,dc=com");

        assertThat(entry.getGroups()).hasSize(2).contains("Engineers", "Whitespace Engineers");
    }

    @Test
    public void testPosixGroupLookup() throws Exception {
        final LdapEntry entry = connector.search(connection,
                "ou=users,dc=example,dc=com",
                "(&(objectClass=posixAccount)(uid={0}))",
                "cn",
                "john",
                false,
                "ou=groups,dc=example,dc=com",
                "cn",
                "(objectClass=posixGroup)");

        assertThat(entry).isNotNull();
        assertThat(entry.getDn())
                .isNotNull()
                .isEqualTo("cn=John Doe,ou=users,dc=example,dc=com");

        assertThat(entry.getGroups()).hasSize(1).contains("Developers");
    }

    @Test
    public void testAllGroupClassesLookup() throws Exception {
        final LdapEntry entry = connector.search(connection,
                "ou=users,dc=example,dc=com",
                "(&(objectClass=posixAccount)(uid={0}))",
                "cn",
                "john",
                false,
                "ou=groups,dc=example,dc=com",
                "cn",
                "(|(objectClass=posixGroup)(objectClass=groupOfNames)(objectclass=groupOfUniqueNames))");

        assertThat(entry).isNotNull();
        assertThat(entry.getDn())
                .isNotNull()
                .isEqualTo("cn=John Doe,ou=users,dc=example,dc=com");

        assertThat(entry.getGroups())
                .hasSize(4)
                .contains("Developers", "QA", "Engineers", "Whitespace Engineers");
    }

    @Test
    public void testListGroups() throws Exception {
        final Set<String> groups = connector.listGroups(connection, "ou=groups,dc=example,dc=com", "(objectClass=top)", "cn");

        assertThat(groups)
                .hasSize(4)
                .contains("Developers", "QA", "Engineers", "Whitespace Engineers");
    }

    @Test
    public void testFindGroupsWithWhitespace() throws Exception {
        final LdapEntry ldapEntry1 = new LdapEntry();
        ldapEntry1.setDn("cn=John Doe,ou=users,dc=example,dc=com");
        ldapEntry1.put("uid", "john");

        final LdapEntry ldapEntry2 = new LdapEntry();
        ldapEntry2.setDn("cn=John Doe,  ou=users, dc=example, dc=com");
        ldapEntry2.put("uid", "john");

        final Set<String> groups1 = connector.findGroups(connection,
                "ou=groups,dc=example,dc=com",
                "(objectClass=groupOfUniqueNames)",
                "cn",
                ldapEntry1);
        final Set<String> groups2 = connector.findGroups(connection,
                "ou=groups,dc=example,dc=com",
                "(objectClass=groupOfUniqueNames)",
                "cn",
                ldapEntry2);

        assertThat(groups1).hasSize(2).containsOnly("Whitespace Engineers", "Engineers");
        assertThat(groups2).hasSize(2).containsOnly("Whitespace Engineers", "Engineers");
    }

    @Test
    public void authenticateThrowsIllegalArgumentExceptionIfPrincipalIsNull() throws LdapException {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Binding with empty principal is forbidden.");
        connector.authenticate(connection, null, "secret");
    }

    @Test
    public void authenticateThrowsIllegalArgumentExceptionIfPrincipalIsEmpty() throws LdapException {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Binding with empty principal is forbidden.");
        connector.authenticate(connection, "", "secret");
    }

    @Test
    public void authenticateThrowsIllegalArgumentExceptionIfCredentialsAreNull() throws LdapException {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Binding with empty credentials is forbidden.");
        connector.authenticate(connection, "principal", null);
    }

    @Test
    public void authenticateThrowsIllegalArgumentExceptionIfCredentialsAreEmpty() throws LdapException {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Binding with empty credentials is forbidden.");
        connector.authenticate(connection, "principal", "");
    }
}

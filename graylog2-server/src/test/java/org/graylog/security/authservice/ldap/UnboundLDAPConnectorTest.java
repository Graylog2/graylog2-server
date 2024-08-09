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
package org.graylog.security.authservice.ldap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifFiles;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.annotations.LoadSchema;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.apache.directory.server.core.partition.impl.avl.AvlPartition;
import org.apache.directory.server.ldap.LdapServer;
import org.graylog.testing.ldap.LDAPTestUtils;
import org.graylog2.ApacheDirectoryTestServiceFactory;
import org.graylog2.configuration.TLSProtocolsConfiguration;
import org.graylog2.security.TrustManagerProvider;
import org.graylog2.security.encryption.EncryptedValue;
import org.graylog2.security.encryption.EncryptedValueService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@ExtendWith(ApacheDSTestExtension.class)
@CreateLdapServer(transports = {
        @CreateTransport(protocol = "LDAP")
})
@CreateDS(
        name = "LdapConnectorTest",
        factory = ApacheDirectoryTestServiceFactory.class, // Ensures a unique storage location
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
@ApplyLdifFiles(LDAPTestUtils.BASE_LDIF)
public class UnboundLDAPConnectorTest extends AbstractLdapTestUnit {
    private static final Set<String> ENABLED_TLS_PROTOCOLS = ImmutableSet.of("TLSv1.2", "TLSv1.3");
    private static final String ADMIN_DN = "uid=admin,ou=system";
    private static final String ADMIN_PASSWORD = "secret";

    private UnboundLDAPConnector connector;
    private LDAPConnection connection;
    private final EncryptedValueService encryptedValueService = new EncryptedValueService("1234567890abcdef");

    @BeforeEach
    public void setUp() throws Exception {
        final LdapServer server = getLdapServer();
        final LDAPConnectorConfig.LDAPServer unreachableServer = LDAPConnectorConfig.LDAPServer.create("localhost", 9);
        final LDAPConnectorConfig.LDAPServer ldapServer = LDAPConnectorConfig.LDAPServer.create("localhost", server.getPort());
        final LDAPConnectorConfig connectorConfig = LDAPConnectorConfig.builder()
                .systemUsername(ADMIN_DN)
                .systemPassword(encryptedValueService.encrypt(ADMIN_PASSWORD))
                .transportSecurity(LDAPTransportSecurity.NONE)
                .verifyCertificates(false)
                .serverList(ImmutableList.of(unreachableServer, ldapServer))
                .build();

        connector = new UnboundLDAPConnector(10000, new TLSProtocolsConfiguration(), mock(TrustManagerProvider.class), encryptedValueService);
        connection = connector.connect(connectorConfig);
    }

    @AfterEach
    public void tearDown() throws Exception {
        connection.close();
    }

    @Test
    public void testUserLookup() throws Exception {
        final UnboundLDAPConfig searchConfig = UnboundLDAPConfig.builder()
                .userSearchBase("ou=users,dc=example,dc=com")
                .userSearchPattern("(&(objectClass=posixAccount)(uid={0}))")
                .userUniqueIdAttribute("entryUUID")
                .userNameAttribute("uid")
                .userFullNameAttribute("cn")
                .emailAttributes(new ArrayList<>())
                .build();
        final LDAPUser entry = connector.searchUserByPrincipal(connection, searchConfig, "john").orElse(null);

        assertThat(entry).isNotNull();
        assertThat(entry.dn())
                .isNotNull()
                .isEqualTo("cn=John Doe,ou=users,dc=example,dc=com");
        assertThat(new String(Base64.getDecoder().decode(entry.base64UniqueId()), StandardCharsets.UTF_8))
                .isNotBlank()
                .matches("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}");
    }

    @Test
    public void testAuthenticateSuccess() throws LDAPException {
        final boolean authenticated = connector.authenticate(connection, "cn=John Doe,ou=users,dc=example,dc=com", encryptedValueService.encrypt("test"));
        assertThat(authenticated).isTrue();
    }

    @Test
    public void testAuthenticateFail() throws LDAPException {
        final boolean authenticated = connector.authenticate(connection, "cn=John Doe,ou=users,dc=example,dc=com", encryptedValueService.encrypt("wrongpass"));
        assertThat(authenticated).isFalse();
    }

    @Test
    public void authenticateThrowsIllegalArgumentExceptionIfPrincipalIsNull() throws LDAPException {
        assertThatThrownBy(() -> connector.authenticate(connection, null, encryptedValueService.encrypt("secret")))
                .hasMessageContaining("Binding with empty principal is forbidden.")
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void authenticateThrowsIllegalArgumentExceptionIfPrincipalIsEmpty() throws LDAPException {
        assertThatThrownBy(() -> connector.authenticate(connection, "", encryptedValueService.encrypt("secret")))
                .hasMessageContaining("Binding with empty principal is forbidden.")
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void authenticateThrowsIllegalArgumentExceptionIfCredentialsAreNull() throws LDAPException {
        //noinspection DataFlowIssue
        assertThatThrownBy( () -> connector.authenticate(connection, "principal", null))
                .hasMessageContaining("Binding with null credentials is forbidden.")
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void authenticateThrowsIllegalArgumentExceptionIfCredentialsAreEmpty() throws LDAPException {
        assertThatThrownBy(() -> connector.authenticate(connection, "principal", EncryptedValue.createUnset()))
                .hasMessageContaining("Binding with empty credentials is forbidden.")
                .isInstanceOf(IllegalArgumentException.class);
    }
}

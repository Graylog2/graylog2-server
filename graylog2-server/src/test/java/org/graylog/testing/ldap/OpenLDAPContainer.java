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
package org.graylog.testing.ldap;

import org.graylog.security.authservice.ldap.LDAPConnectorConfig;
import org.graylog.security.authservice.ldap.LDAPTransportSecurity;
import org.graylog2.security.encryption.EncryptedValueService;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;
import java.util.Collections;
import java.util.Locale;

/**
 * Creates an OpenLDAP server container configured with users and (nested) groups.
 */
public class OpenLDAPContainer extends GenericContainer<OpenLDAPContainer> {
    private static final String IMAGE_NAME = "osixia/openldap:1.4.0";
    private static final String CONTAINER_CERTS_PATH = "/container/service/slapd/assets/certs";
    private static final int PORT = 389;
    private static final int TLS_PORT = 636;

    private static final String BIND_DN = "cn=admin,dc=example,dc=com";
    private static final String BIND_PASSWORD = "admin";

    public static OpenLDAPContainer create() {
        return new OpenLDAPContainer();
    }

    public static OpenLDAPContainer createWithTLS() {
        return new OpenLDAPContainer()
                .withEnv("LDAP_TLS", "true")
                .withEnv("LDAP_TLS_VERIFY_CLIENT", "allow")
                .withEnv("LDAP_TLS_CRT_FILENAME", "server-cert.pem")
                .withEnv("LDAP_TLS_KEY_FILENAME", "server-key.pem")
                .withEnv("LDAP_TLS_CA_CRT_FILENAME", "CA-cert.pem")
                .withEnv("LDAP_TLS_DH_PARAM_FILENAME", "dhparam.pem")
                .withFileSystemBind(LDAPTestUtils.testTLSCertsPath("server-cert.pem"), CONTAINER_CERTS_PATH + "/server-cert.pem", BindMode.READ_ONLY)
                .withFileSystemBind(LDAPTestUtils.testTLSCertsPath("server-key.pem"), CONTAINER_CERTS_PATH + "/server-key.pem", BindMode.READ_ONLY)
                .withFileSystemBind(LDAPTestUtils.testTLSCertsPath("CA-cert.pem"), CONTAINER_CERTS_PATH + "/CA-cert.pem", BindMode.READ_ONLY)
                .withFileSystemBind(LDAPTestUtils.testTLSCertsPath("dhparam.pem"), CONTAINER_CERTS_PATH + "/dhparam.pem", BindMode.READ_ONLY);
    }

    public OpenLDAPContainer() {
        super(IMAGE_NAME);

        waitingFor(Wait.forLogMessage(".*slapd starting.*", 1));
        withCommand("--copy-service");
        withEnv("LDAP_ORGANISATION", "Example, Inc.");
        withEnv("LDAP_DOMAIN", "example.com");
        withEnv("LDAP_TLS", "false");
        withFileSystemBind(
                LDAPTestUtils.getResourcePath(LDAPTestUtils.NESTED_GROUPS_LDIF),
                "/container/service/slapd/assets/config/bootstrap/ldif/custom/custom.ldif",
                BindMode.READ_ONLY
        );
        withNetwork(Network.newNetwork());
        withNetworkAliases("openldap");
        withStartupTimeout(Duration.ofSeconds(10));
    }

    /**
     * Returns the {@link LDAPConnectorConfig} for the running container.
     */
    public LDAPConnectorConfig createLDAPConnectorConfig(EncryptedValueService encryptedValueService) {
        return LDAPConnectorConfig.builder()
                .systemUsername(bindDn())
                .systemPassword(encryptedValueService.encrypt(bindPassword()))
                .serverList(Collections.singletonList(LDAPConnectorConfig.LDAPServer.fromUrl(ldapURL())))
                .transportSecurity(LDAPTransportSecurity.NONE)
                .verifyCertificates(false)
                .build();
    }

    public String bindDn() {
        return BIND_DN;
    }

    public String bindPassword() {
        return BIND_PASSWORD;
    }

    /**
     * The mapped LDAP port for plain text or StartTLS connections.
     */
    public int ldapPort() {
        return getMappedPort(PORT);
    }

    /**
     * The mapped LDAP port for TLS connections.
     */
    public int ldapsPort() {
        return getMappedPort(TLS_PORT);
    }

    /**
     * Returns an LDAP URL string for plain text or StartTLS connections.
     */
    public String ldapURL() {
        return String.format(Locale.US, "ldap://127.0.0.1:%s/", ldapPort());
    }

    /**
     * Returns an LDAP URL string for TLS connections.
     */
    public String ldapsURL() {
        return String.format(Locale.US, "ldaps://127.0.0.1:%s/", ldapsPort());
    }
}

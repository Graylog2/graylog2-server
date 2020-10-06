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
package org.graylog.testing.ldap;

import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;

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
                .withFileSystemBind(LDAPTestUtils.testTLSCertsPath(), CONTAINER_CERTS_PATH, BindMode.READ_ONLY);
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

    public String bindDn() {
        return BIND_DN;
    }

    public String bindPassword() {
        return BIND_PASSWORD;
    }

    public int ldapPort() {
        return getMappedPort(PORT);
    }

    public int ldapsPort() {
        return getMappedPort(TLS_PORT);
    }
}

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

import com.google.common.io.Resources;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.security.KeyStore;

public class LDAPTestUtils {
    private static final String RESOURCE_ROOT = "org/graylog/testing/ldap";

    public static final String BASE_LDIF = RESOURCE_ROOT + "/ldif/base.ldif";

    public static String testTLSCertsPath() {
        final URL resourceUrl = Resources.getResource(RESOURCE_ROOT + "/certs");
        try {
            return Paths.get(resourceUrl.toURI()).toAbsolutePath().toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static KeyStore getKeystore(String filename) {
        try {
            final KeyStore keystore = KeyStore.getInstance("JKS");
            keystore.load(Resources.getResource(RESOURCE_ROOT + "/" + filename).openStream(), "changeit".toCharArray());
            return keystore;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

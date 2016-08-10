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
package org.graylog2.plugin.inputs.transports.util;

import com.google.common.collect.ImmutableMap;
import org.jboss.netty.handler.ssl.SslHandler;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import java.io.File;
import java.net.URISyntaxException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class KeyUtilTest {
    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    private static final Map<String, String> CERTIFICATES = ImmutableMap.of(
            "RSA", "server.crt.rsa",
            "DSA", "server.crt.dsa",
            "ECDSA", "server.crt.ecdsa"
    );

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                // algorithm, key filename, password, exception class, exception text
                {"RSA", "server.key.pem.ue.pkcs1.rsa", null, IllegalArgumentException.class, "Unsupported key type PKCS#1, please convert to PKCS#8"},
                {"RSA", "server.key.pem.e.pkcs8.rsa", "test", null, null},
                {"RSA", "server.key.pem.ue.pkcs8.rsa", null, null, null},
                {"RSA", "server.key.der.e.pkcs8.rsa", "test", null, null},
                {"RSA", "server.key.der.ue.pkcs8.rsa", null, null, null},

                {"DSA", "server.key.pem.ue.pkcs1.dsa", null, IllegalArgumentException.class, "Unsupported key type PKCS#1, please convert to PKCS#8"},
                {"DSA", "server.key.pem.e.pkcs8.dsa", "test", null, null},
                {"DSA", "server.key.pem.ue.pkcs8.dsa", null, null, null},
                {"DSA", "server.key.der.e.pkcs8.dsa", "test", null, null},
                {"DSA", "server.key.der.ue.pkcs8.dsa", null, null, null},

                {"ECDSA", "server.key.pem.ue.pkcs1.ecdsa", null, IllegalArgumentException.class, "Unsupported key type PKCS#1, please convert to PKCS#8"},
                {"ECDSA", "server.key.pem.e.pkcs8.ecdsa", "test", null, null},
                {"ECDSA", "server.key.pem.ue.pkcs8.ecdsa", null, null, null},
                {"ECDSA", "server.key.der.e.pkcs8.ecdsa", "test", null, null},
                {"ECDSA", "server.key.der.ue.pkcs8.ecdsa", null, null, null},

                {"RSA", "server.key.invalid", null, IllegalArgumentException.class, "Unsupported key type: "},
        });
    }

    private final String keyAlgorithm;
    private final String keyFileName;
    private final String keyPassword;
    private final Class<? extends Exception> exceptionClass;
    private final String exceptionMessage;

    public KeyUtilTest(String keyAlgorithm,
                       String keyFileName,
                       String keyPassword,
                       Class<? extends Exception> exceptionClass,
                       String exceptionMessage) {
        this.keyAlgorithm = requireNonNull(keyAlgorithm);
        this.keyFileName = requireNonNull(keyFileName);
        this.keyPassword = keyPassword;
        this.exceptionClass = exceptionClass;
        this.exceptionMessage = exceptionMessage;
    }

    private File resourceToFile(String fileName) throws URISyntaxException {
        return new File(getClass().getResource(fileName).toURI());
    }

    @Test
    public void testLoadPrivateKey() throws Exception {
        if (exceptionClass != null) {
            expectedException.expect(exceptionClass);
            expectedException.expectMessage(exceptionMessage);
        }

        final File keyFile = resourceToFile(keyFileName);
        final PrivateKey privateKey = KeyUtil.loadPrivateKey(keyFile, keyPassword);
        assertThat(privateKey).isNotNull();
    }

    @Test
    public void testCreateNettySslHandler() throws Exception {
        if (exceptionClass != null) {
            expectedException.expect(exceptionClass);
            expectedException.expectMessage(exceptionMessage);
        }

        final File keyFile = resourceToFile(keyFileName);
        final File certFile = resourceToFile(CERTIFICATES.get(keyAlgorithm));
        final KeyManager[] keyManagers = KeyUtil.initKeyStore(keyFile, certFile, keyPassword);

        final SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagers, new TrustManager[0], new SecureRandom());
        assertThat(sslContext.getProtocol()).isEqualTo("TLS");

        final SSLEngine sslEngine = sslContext.createSSLEngine();

        assertThat(sslEngine.getEnabledCipherSuites()).isNotEmpty();
        assertThat(sslEngine.getEnabledProtocols()).isNotEmpty();

        final SslHandler sslHandler = new SslHandler(sslEngine);
        assertThat(sslHandler).isNotNull();
    }
}

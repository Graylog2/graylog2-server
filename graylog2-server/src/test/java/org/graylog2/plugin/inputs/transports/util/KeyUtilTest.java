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

import org.jboss.netty.handler.ssl.SslContext;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class KeyUtilTest {
    private static final String SERVER_KEY_PEM_E_PKCS8_RSA = "server.key.pem.e.pkcs8.rsa";
    private static final String SERVER_KEY_PEM_E_PKCS8_DSA = "server.key.pem.e.pkcs8.dsa";
    private static final String X509 = "X509";
    private static final String DIR = "dir";
    private static final String SERVER_KEY_PEM_UE_PKCS1_RSA = "server.key.pem.ue.pkcs1.rsa";
    private static final String SERVER_KEY_PEM_UE_PKCS1_DSA = "server.key.pem.ue.pkcs1.dsa";
    private static final String SERVER_KEY_DER_E_PKCS8_RSA = "server.key.der.e.pkcs8.rsa";
    private static final String SERVER_KEY_DER_E_PKCS8_DSA = "server.key.der.e.pkcs8.dsa";
    private static final String SERVER_KEY_PEM_UE_PKCS8_RSA = "server.key.pem.ue.pkcs8.rsa";
    private static final String SERVER_KEY_PEM_UE_PKCS8_DSA = "server.key.pem.ue.pkcs8.dsa";
    private static final String SERVER_KEY_DER_UE_PKCS8_RSA = "server.key.der.ue.pkcs8.rsa";
    private static final String SERVER_KEY_DER_UE_PKCS8_DSA = "server.key.der.ue.pkcs8.dsa";
    private static final String SERVER_CRT_RSA = "server.crt.rsa";
    private static final String SERVER_CRT_DSA = "server.crt.dsa";

    @Test
    public void testLoadCertificateFiles()
            throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException, URISyntaxException {
        final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null, null);
        KeyUtil.loadCertificates(ks, resourceToFile(SERVER_CRT_RSA), CertificateFactory.getInstance(X509));
        KeyUtil.loadCertificates(ks, resourceToFile(SERVER_CRT_DSA), CertificateFactory.getInstance(X509));
        assertEquals(2, ks.size());
    }

    private File resourceToFile(String fileName) throws URISyntaxException {
        return new File(getClass().getResource(fileName).toURI());
    }

    @Test
    public void testLoadCertificateDir()
            throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException, URISyntaxException {
        final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null, null);
        KeyUtil.loadCertificates(ks, resourceToFile(DIR), CertificateFactory.getInstance(X509));
        assertEquals(2, ks.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLoadPrivateKeyPemUnencryptedPKCS1RSA() throws IOException, GeneralSecurityException, URISyntaxException {
        final File keyFile = resourceToFile(SERVER_KEY_PEM_UE_PKCS1_RSA);
        KeyUtil.loadPrivateKey(keyFile, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLoadPrivateKeyPemUnencryptedPKCS1DSA() throws IOException, GeneralSecurityException, URISyntaxException {
        final File keyFile = resourceToFile(SERVER_KEY_PEM_UE_PKCS1_DSA);
        KeyUtil.loadPrivateKey(keyFile, null);
    }

    @Test
    public void testLoadPrivateKeyPemUnencryptedPKCS8RSA() throws IOException, GeneralSecurityException, URISyntaxException {
        final File keyFile = resourceToFile(SERVER_KEY_PEM_UE_PKCS8_RSA);
        SslContext.newServerContext(resourceToFile(SERVER_CRT_RSA), keyFile, null);

        final PrivateKey privateKey = KeyUtil.loadPrivateKey(keyFile, null);
        assertNotNull(privateKey);
    }

    @Test
    public void testLoadPrivateKeyPemUnencryptedPKCS8DSA() throws IOException, GeneralSecurityException, URISyntaxException {
        final File keyFile = resourceToFile(SERVER_KEY_PEM_UE_PKCS8_DSA);
        SslContext.newServerContext(resourceToFile(SERVER_CRT_DSA), keyFile, null);

        final PrivateKey privateKey = KeyUtil.loadPrivateKey(keyFile, null);
        assertNotNull(privateKey);
    }

    @Test
    public void testLoadPrivateKeyPemPKCSEncryptedRSA() throws IOException, GeneralSecurityException, URISyntaxException {
        final File keyFile = resourceToFile(SERVER_KEY_PEM_E_PKCS8_RSA);
        SslContext.newServerContext(resourceToFile(SERVER_CRT_RSA), keyFile, "test");

        final PrivateKey privateKey = KeyUtil.loadPrivateKey(keyFile, "test");
        assertNotNull(privateKey);
    }

    @Test
    public void testLoadPrivateKeyPemPKCSEncryptedDSA() throws IOException, GeneralSecurityException, URISyntaxException {
        final File keyFile = resourceToFile(SERVER_KEY_PEM_E_PKCS8_DSA);
        SslContext.newServerContext(resourceToFile(SERVER_CRT_DSA), keyFile, "test");

        final PrivateKey privateKey = KeyUtil.loadPrivateKey(keyFile, "test");
        assertNotNull(privateKey);
    }

    @Test
    public void testLoadPrivateKeyDerPKCS8EncryptedRSA() throws IOException, GeneralSecurityException, URISyntaxException {
        final File keyFile = resourceToFile(SERVER_KEY_DER_E_PKCS8_RSA);
        try {
            SslContext.newServerContext(resourceToFile(SERVER_CRT_RSA), keyFile, "test");
            fail();
        } catch (Exception e) {
            // expected, not supported by netty
        }
        final PrivateKey privateKey = KeyUtil.loadPrivateKey(keyFile, "test");
        assertNotNull(privateKey);
    }

    @Test
    public void testLoadPrivateKeyDerPKCS8EncryptedDSA() throws IOException, GeneralSecurityException, URISyntaxException {
        final File keyFile = resourceToFile(SERVER_KEY_DER_E_PKCS8_DSA);
        try {
            SslContext.newServerContext(resourceToFile(SERVER_CRT_DSA), keyFile, "test");
            fail();
        } catch (Exception e) {
            // expected, not supported by netty
        }
        final PrivateKey privateKey = KeyUtil.loadPrivateKey(keyFile, "test");
        assertNotNull(privateKey);
    }

    @Test
    public void testLoadPrivateKeyDerPKCS8UnencryptedRSA() throws IOException, GeneralSecurityException, URISyntaxException {
        final File keyFile = resourceToFile(SERVER_KEY_DER_UE_PKCS8_RSA);
        try {
            SslContext.newServerContext(resourceToFile(SERVER_CRT_RSA), keyFile, null);
            fail();
        } catch (Exception e) {
            // expected, not supported by netty
        }

        final PrivateKey privateKey = KeyUtil.loadPrivateKey(keyFile, null);
        assertNotNull(privateKey);
    }

    @Test
    public void testLoadPrivateKeyDerPKCS8UnencryptedDSA() throws IOException, GeneralSecurityException, URISyntaxException {
        final File keyFile = resourceToFile(SERVER_KEY_DER_UE_PKCS8_DSA);
        try {
            SslContext.newServerContext(resourceToFile(SERVER_CRT_DSA), keyFile, null);
            fail();
        } catch (Exception e) {
            // expected, not supported by netty
        }

        final PrivateKey privateKey = KeyUtil.loadPrivateKey(keyFile, null);
        assertNotNull(privateKey);
    }
}

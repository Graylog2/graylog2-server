/**
 * The MIT License
 * Copyright (c) 2012 Graylog, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.graylog2.plugin.inputs.transports.util;

import org.jboss.netty.handler.ssl.SslContext;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
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
    private static final String SERVER_KEY_PEM_E_PKCS8 = "server.key.pem.e.pkcs8";
    private static final String X509 = "X509";
    private static final String DIR = "dir";
    private static final String SERVER_KEY_PEM_UE_PKCS1 = "server.key.pem.ue.pkcs1";
    private static final String SERVER_KEY_DER_E_PKCS8 = "server.key.der.e.pkcs8";
    private static final String SERVER_KEY_PEM_UE_PKCS8 = "server.key.pem.ue.pkcs8";
    private static final String SERVER_KEY_DER_UE_PKCS8 = "server.key.der.ue.pkcs8";
    private static final String SERVER_CRT = "server.crt";

    @Test
    public void testLoadCertificateFile()
            throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {
        final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null, null);
        KeyUtil.loadCertificates(ks, resourceToFile(SERVER_CRT), CertificateFactory.getInstance(X509));
        assertEquals(1, ks.size());
    }

    private File resourceToFile(String fileName) {
        return new File(getClass().getResource(fileName).getFile());
    }

    @Test
    public void testLoadCertificateDir()
            throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {
        final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null, null);
        KeyUtil.loadCertificates(ks, resourceToFile(DIR), CertificateFactory.getInstance(X509));
        assertEquals(1, ks.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLoadPrivateKeyPemUnencryptedPKCS1() throws IOException, GeneralSecurityException {
        final File keyFile = resourceToFile(SERVER_KEY_PEM_UE_PKCS1);
        KeyUtil.loadPrivateKey(keyFile, null);
    }

    @Test
    public void testLoadPrivateKeyPemUnencryptedPKCS8() throws IOException, GeneralSecurityException {
        final File keyFile = resourceToFile(SERVER_KEY_PEM_UE_PKCS8);
        SslContext.newServerContext(resourceToFile(SERVER_CRT), keyFile, null);

        final PrivateKey privateKey = KeyUtil.loadPrivateKey(keyFile, null);
        assertNotNull(privateKey);
    }

    @Test
    public void testLoadPrivateKeyPemPKCSEncrypted() throws IOException, GeneralSecurityException {
        final File keyFile = resourceToFile(SERVER_KEY_PEM_E_PKCS8);
        SslContext.newServerContext(resourceToFile(SERVER_CRT), keyFile, "test");

        final PrivateKey privateKey = KeyUtil.loadPrivateKey(keyFile, "test");
        assertNotNull(privateKey);
    }

    @Test
    public void testLoadPrivateKeyDerPKCS8Encrypted() throws IOException, GeneralSecurityException {
        final File keyFile = resourceToFile(SERVER_KEY_DER_E_PKCS8);
        try {
            SslContext.newServerContext(resourceToFile(SERVER_CRT), keyFile, "test");
            fail();
        } catch (Exception e) {
            // expected, not supported by netty
        }
        final PrivateKey privateKey = KeyUtil.loadPrivateKey(keyFile, "test");
        assertNotNull(privateKey);
    }

    @Test
    public void testLoadPrivateKeyDerPKCS8Unencrypted() throws IOException, GeneralSecurityException {
        final File keyFile = resourceToFile(SERVER_KEY_DER_UE_PKCS8);
        try {
            SslContext.newServerContext(resourceToFile(SERVER_CRT), keyFile, null);
            fail();
        } catch (Exception e) {
            // expected, not supported by netty
        }

        final PrivateKey privateKey = KeyUtil.loadPrivateKey(keyFile, null);
        assertNotNull(privateKey);
    }
}

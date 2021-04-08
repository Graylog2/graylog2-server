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
package org.graylog2.shared.security.tls;

import com.google.common.base.Strings;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.PKCS8Generator;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JcaPKCS8Generator;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8EncryptorBuilder;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.OutputEncryptor;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.PKCSException;
import org.bouncycastle.util.io.pem.PemObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;

public final class KeyStoreUtils {
    private KeyStoreUtils() {
    }

    public static byte[] getBytes(KeyStore keyStore, char[] password) throws GeneralSecurityException, IOException {
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        keyStore.store(stream, password);

        return stream.toByteArray();
    }

    /**
     * Build a password-encrypted PKCS8 private key and write it to a PEM file in the temp directory.
     * Caller is responsible for ensuring that the temp directory is writable. The file will be deleted
     * when the VM exits.
     * @param password to protect the key
     * @param key encrypt this key
     * @return PEM file
     * @throws GeneralSecurityException
     */
    public static File generatePKSC8PrivateKey(char[] password, PrivateKey key) throws GeneralSecurityException {
        try {
            JceOpenSSLPKCS8EncryptorBuilder encryptorBuilder =
                    new JceOpenSSLPKCS8EncryptorBuilder(PKCS8Generator.AES_256_CBC)
                    .setRandom(new SecureRandom())
                    .setPasssword(password);
            OutputEncryptor encryptor = encryptorBuilder.build();

            // construct object to create the PKCS8 object from the private key and encryptor
            PemObject pemObj = new JcaPKCS8Generator(key, encryptor).generate();
            StringWriter stringWriter = new StringWriter();
            try (JcaPEMWriter pemWriter = new JcaPEMWriter(stringWriter)) {
                pemWriter.writeObject(pemObj);
            }

            // write PKCS8 to file
            String pkcs8Key = stringWriter.toString();
            File tmpFile = File.createTempFile("key", ".tmp");
            tmpFile.deleteOnExit();
            FileOutputStream fos = new FileOutputStream(tmpFile);
            fos.write(pkcs8Key.getBytes(StandardCharsets.UTF_8));
            fos.flush();
            fos.close();
            return tmpFile;
        } catch (IOException | OperatorCreationException e) {
            throw new GeneralSecurityException(e);
        }
    }

    /**
     * Obtain a private key from a PKS8 PEM file, which is optionally password-protected.
     * @param password password to decrypt the file - it may be null or empty in case of an unencrypted file
     * @param keyFile the key file
     * @return the corresponding private key
     */
    public static PrivateKey privateKeyFromFile(String password, File keyFile) throws IOException, PKCSException, OperatorCreationException {
        PrivateKey privateKey;

        JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
        PEMParser pemParser = new PEMParser(new FileReader(keyFile));
        Object object = pemParser.readObject();
        if (object instanceof PKCS8EncryptedPrivateKeyInfo) {
            PKCS8EncryptedPrivateKeyInfo pInfo = (PKCS8EncryptedPrivateKeyInfo) object;
            InputDecryptorProvider provider =
                    new JceOpenSSLPKCS8DecryptorProviderBuilder()
                            .build(Strings.nullToEmpty(password).toCharArray());
            PrivateKeyInfo info = pInfo.decryptPrivateKeyInfo(provider);
            privateKey = converter.getPrivateKey(info);
        }
        else if (object instanceof PrivateKeyInfo) {
            privateKey = converter.getPrivateKey((PrivateKeyInfo) object);
        }
        else {
            throw new PKCSException("Encountered unexpected object type: " + object.getClass().getName());
        }

        return privateKey;
    }

    /**
     * Obtain a certificate chain from an X.509 file.
     * @param certFile the X.509 file
     * @return an iterable collection of X.509 certificates
     */
    public static Iterable<X509Certificate> keyCertChainFromFile(File certFile) throws CertificateException, NoSuchProviderException, FileNotFoundException {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509", "BC");
        FileInputStream is = new FileInputStream(certFile);
        Collection<X509Certificate> keyCertChain = (Collection<X509Certificate>) certificateFactory.generateCertificates(is);
        return keyCertChain;
    }
}

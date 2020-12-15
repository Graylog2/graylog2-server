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

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.PKCSException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;

public final class PemKeyStore {
    private static final char[] EMPTY_CHAR_ARRAY = new char[0];

    /**
     * Generates a key specification for an (encrypted) private key.
     *
     * @param password characters, if {@code null} or empty an unencrypted key is assumed
     * @param key      bytes of the DER encoded private key
     * @return a key specification
     * @throws IOException                        if parsing {@code key} fails
     * @throws PKCSException                if the decryption key based on {@code password} cannot be used to decrypt
     *                                            {@code key}
     * @throws OperatorCreationException    if the decryption algorithm parameters are somehow faulty
     */
    protected static PKCS8EncodedKeySpec generateKeySpec(char[] password, byte[] key)
            throws IOException, PKCSException, OperatorCreationException {

        if (password == null || password.length == 0) {
            return new PKCS8EncodedKeySpec(key);
        }

        final PKCS8EncryptedPrivateKeyInfo privateKeyInfo = new PKCS8EncryptedPrivateKeyInfo(key);
        final InputDecryptorProvider decProv = new JceOpenSSLPKCS8DecryptorProviderBuilder().setProvider("BC").build(password);
        PrivateKeyInfo pkInfo = privateKeyInfo.decryptPrivateKeyInfo(decProv);
        PrivateKey privKey = new JcaPEMKeyConverter().setProvider("BC").getPrivateKey(pkInfo);
        return new PKCS8EncodedKeySpec(privKey.getEncoded());
    }

    /**
     * Generates a new {@link KeyStore}.
     *
     * @param certChainFile    a X.509 certificate chain file in PEM format,
     * @param keyFile          a PKCS#8 private key file in PEM format,
     * @param keyPasswordChars the password of the {@code keyFile}.
     *                         {@code null} if it's not password-protected.
     * @throws GeneralSecurityException on any error regarding key generation
     * @return generated {@link KeyStore}.
     */
    public static KeyStore buildKeyStore(Path certChainFile, Path keyFile, char[] keyPasswordChars) throws GeneralSecurityException {
        try {
            return doBuildKeyStore(certChainFile, keyFile, keyPasswordChars);
        } catch (KeyStoreException | NoSuchAlgorithmException | InvalidKeySpecException | CertificateException |
                KeyException | IOException | PKCSException | OperatorCreationException e) {
            throw new GeneralSecurityException(e);
        }
    }

    @SuppressWarnings("InsecureCryptoUsage")
    private static KeyStore doBuildKeyStore(Path certChainFile, Path keyFile, char[] keyPasswordChars)
            throws KeyStoreException, NoSuchAlgorithmException, InvalidKeySpecException,
            CertificateException, KeyException, IOException, PKCSException, OperatorCreationException {
        char[] password = keyPasswordChars == null ? EMPTY_CHAR_ARRAY : keyPasswordChars;

        byte[] encodedKey = PemReader.readPrivateKey(keyFile);
        final PKCS8EncodedKeySpec encodedKeySpec = generateKeySpec(keyPasswordChars, encodedKey);

        PrivateKey key;
        try {
            key = KeyFactory.getInstance("RSA").generatePrivate(encodedKeySpec);
        } catch (InvalidKeySpecException ignore) {
            try {
                key = KeyFactory.getInstance("DSA").generatePrivate(encodedKeySpec);
            } catch (InvalidKeySpecException ignore2) {
                try {
                    key = KeyFactory.getInstance("EC").generatePrivate(encodedKeySpec);
                } catch (InvalidKeySpecException e) {
                    throw new InvalidKeySpecException("Neither RSA, DSA nor EC worked", e);
                }
            }
        }

        final CertificateFactory cf = CertificateFactory.getInstance("X.509");
        final List<byte[]> certs = PemReader.readCertificates(certChainFile);
        final List<Certificate> certChain = new ArrayList<>(certs.size());

        for (byte[] buf : certs) {
            certChain.add(cf.generateCertificate(new ByteArrayInputStream(buf)));
        }

        final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null, password);
        ks.setKeyEntry("key", key, password, certChain.toArray(new Certificate[certChain.size()]));

        return ks;
    }
}

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
package org.graylog.security.certutil.keystore.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Enumeration;

import static org.graylog.security.certutil.CertConstants.PKCS12;
import static org.graylog.security.certutil.CertConstants.SIGNING_ALGORITHM;

public class KeystoreUtils {
    public static KeyStore newStoreCopyContent(KeyStore originalKeyStore,
                                               char[] currentPassword,
                                               final char[] newPassword) throws GeneralSecurityException, IOException {
        if (newPassword == null) {
            throw new IllegalArgumentException("new password cannot be null");
        }
        KeyStore newKeyStore = KeyStore.getInstance(PKCS12);
        newKeyStore.load(null, newPassword);

        final Enumeration<String> aliases = originalKeyStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (originalKeyStore.entryInstanceOf(alias, KeyStore.PrivateKeyEntry.class)) {
                newKeyStore.setKeyEntry(
                        alias,
                        originalKeyStore.getKey(alias, currentPassword),
                        newPassword,
                        originalKeyStore.getCertificateChain(alias)
                );
            } else if (originalKeyStore.entryInstanceOf(alias, KeyStore.TrustedCertificateEntry.class)) {
                newKeyStore.setCertificateEntry(alias, originalKeyStore.getCertificate(alias));
            } else if (originalKeyStore.entryInstanceOf(alias, KeyStore.SecretKeyEntry.class)) {
                newKeyStore.setEntry(alias,
                        originalKeyStore.getEntry(alias, new KeyStore.PasswordProtection(currentPassword)),
                        new KeyStore.PasswordProtection(newPassword)
                );
            }
        }
        return newKeyStore;
    }

    private static final String SAMPLE_CHALLANGE = "Grayloggers! Grayloggers! Grayloggers! Grayloggers!";

    public static boolean matchingKeys(final PrivateKey privateKey,
                                final PublicKey publicKey) throws GeneralSecurityException {
        Signature sign = Signature.getInstance(SIGNING_ALGORITHM);

        byte[] bytes = SAMPLE_CHALLANGE.getBytes(StandardCharsets.UTF_8);

        sign.initSign(privateKey);
        sign.update(bytes);
        byte[] signature = sign.sign();

        sign.initVerify(publicKey);
        sign.update(bytes);
        return sign.verify(signature);
    }

}

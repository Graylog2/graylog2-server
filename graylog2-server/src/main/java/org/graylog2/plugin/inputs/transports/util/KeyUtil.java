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
package org.graylog2.plugin.inputs.transports.util;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Iterators;
import com.google.common.io.BaseEncoding;
import com.google.common.io.ByteStreams;
import io.netty.handler.ssl.PemPrivateKey;
import io.netty.handler.ssl.PemX509Certificate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Collection;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KeyUtil {
    private static final Logger LOG = LoggerFactory.getLogger(KeyUtil.class);
    private static final Joiner JOINER = Joiner.on(",").skipNulls();
    private static final Pattern KEY_PATTERN = Pattern.compile("-{5}BEGIN (?:(RSA|DSA|EC)? )?(ENCRYPTED )?PRIVATE KEY-{5}\\r?\\n([A-Z0-9a-z+/\\r\\n]+={0,2})\\r?\\n-{5}END (?:(?:RSA|DSA|EC)? )?(?:ENCRYPTED )?PRIVATE KEY-{5}\\r?\\n$", Pattern.MULTILINE);

    public static Collection<? extends Certificate> loadCertificates(Path certificatePath) throws CertificateException, IOException {
        final CertificateFactory cf = CertificateFactory.getInstance("X.509");
        File certFile = certificatePath.toFile();

        if (certFile.isDirectory()) {
            final ByteArrayOutputStream certStream = new ByteArrayOutputStream();
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(certFile.toPath())) {
                for (Path f : ds) {
                    certStream.write(Files.readAllBytes(f));
                }
            }
            return cf.generateCertificates(new ByteArrayInputStream(certStream.toByteArray()));
        } else {
            try (InputStream inputStream = Files.newInputStream(certificatePath)) {
                return cf.generateCertificates(inputStream);
            }
        }
    }

    public static KeyManager[] initKeyStore(File tlsKeyFile, File tlsCertFile, String tlsKeyPassword)
            throws IOException, GeneralSecurityException {
        final KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null, null);
        final Collection<? extends Certificate> certChain = loadCertificates(tlsCertFile.toPath());
        final PrivateKey privateKey = loadPrivateKey(tlsKeyFile, tlsKeyPassword);
        final char[] password = Strings.nullToEmpty(tlsKeyPassword).toCharArray();
        ks.setKeyEntry("key", privateKey, password, certChain.toArray(new Certificate[certChain.size()]));

        if (LOG.isDebugEnabled()) {
            LOG.debug("Private key file: {}", tlsKeyFile);
            LOG.debug("Certificate file: {}", tlsCertFile);
            LOG.debug("Aliases: {}", join(ks.aliases()));
        }

        final KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, password);

        return kmf.getKeyManagers();
    }

    private static String join(Enumeration<String> aliases) {
        return JOINER.join(Iterators.forEnumeration(aliases));
    }

    @VisibleForTesting
    protected static PrivateKey loadPrivateKey(File file, String password) throws IOException, GeneralSecurityException {
        try (final InputStream is = Files.newInputStream(file.toPath())) {
            final byte[] keyBytes = ByteStreams.toByteArray(is);
            final String keyString = new String(keyBytes, StandardCharsets.US_ASCII);
            final Matcher m = KEY_PATTERN.matcher(keyString);
            byte[] encoded = keyBytes;

            if (m.matches()) {
                if (!Strings.isNullOrEmpty(m.group(1))) {
                    throw new IllegalArgumentException("Unsupported key type PKCS#1, please convert to PKCS#8");
                }

                encoded = BaseEncoding.base64().decode(m.group(3).replaceAll("[\\r\\n]", ""));
            }

            final EncodedKeySpec keySpec = createKeySpec(encoded, password);
            if (keySpec == null) {
                throw new IllegalArgumentException("Unsupported key type: " + file);
            }

            final String[] keyAlgorithms = {"RSA", "DSA", "EC"};
            for (String keyAlgorithm : keyAlgorithms) {
                try {
                    @SuppressWarnings("InsecureCryptoUsage") final KeyFactory keyFactory = KeyFactory.getInstance(keyAlgorithm);
                    return keyFactory.generatePrivate(keySpec);
                } catch (InvalidKeySpecException e) {
                    LOG.debug("Loading {} private key from \"{}\" failed", keyAlgorithm, file, e);
                }
            }

            throw new IllegalArgumentException("Unsupported key type: " + file);
        }
    }

    private static PKCS8EncodedKeySpec createKeySpec(byte[] keyBytes, String password)
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException,
            InvalidKeyException, InvalidAlgorithmParameterException {
        if (Strings.isNullOrEmpty(password)) {
            return new PKCS8EncodedKeySpec(keyBytes);
        }

        final EncryptedPrivateKeyInfo pkInfo = new EncryptedPrivateKeyInfo(keyBytes);
        final SecretKeyFactory kf = SecretKeyFactory.getInstance(pkInfo.getAlgName());
        final PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray());
        final SecretKey secretKey = kf.generateSecret(keySpec);

        @SuppressWarnings("InsecureCryptoUsage") final Cipher cipher = Cipher.getInstance(pkInfo.getAlgName());
        cipher.init(Cipher.DECRYPT_MODE, secretKey, pkInfo.getAlgParameters());

        return pkInfo.getKeySpec(cipher);
    }

    public static X509Certificate readCertificate(Path path) throws IOException {
        final byte[] bytes = Files.readAllBytes(path);
        return PemX509Certificate.valueOf(bytes);
    }

    public static PrivateKey readPrivateKey(Path path) throws IOException {
        final byte[] bytes = Files.readAllBytes(path);
        return PemPrivateKey.valueOf(bytes);
    }
}

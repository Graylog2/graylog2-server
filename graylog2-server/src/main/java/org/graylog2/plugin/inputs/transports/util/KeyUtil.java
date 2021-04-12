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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
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
import java.security.SecureRandom;
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

    public static X509Certificate[] loadX509Certificates(Path certificatePath) throws CertificateException, IOException {
        return loadCertificates(certificatePath).stream()
                .filter(certificate -> certificate instanceof X509Certificate)
                .map(certificate -> (X509Certificate) certificate)
                .toArray(X509Certificate[]::new);
    }

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

    /**
     * Build a password-encrypted PKCS8 private key and write it to a PEM file in the temp directory.
     * Caller is responsible for ensuring that the temp directory is writable. The file will be deleted
     * when the VM exits.
     * @param password to protect the key
     * @param key encrypt this key
     * @return PEM file
     * @throws GeneralSecurityException
     */
    public static File generatePKSC8FromPrivateKey(char[] password, PrivateKey key) throws GeneralSecurityException {
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
            File tmpFile = File.createTempFile("key", ".key");
            tmpFile.deleteOnExit();
            try (FileOutputStream fos = new FileOutputStream(tmpFile)) {
                fos.write(pkcs8Key.getBytes(StandardCharsets.UTF_8));
                fos.flush();
            }
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

        // Be sure to specify charset for reader - don't use plain FileReader
        Object object;
        final InputStream inputStream = Files.newInputStream(keyFile.toPath());
        final InputStreamReader fileReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        try (PEMParser pemParser = new PEMParser(fileReader)) {
            object = pemParser.readObject();
        }

        if (object instanceof PKCS8EncryptedPrivateKeyInfo) {
            PKCS8EncryptedPrivateKeyInfo pInfo = (PKCS8EncryptedPrivateKeyInfo) object;
            JceOpenSSLPKCS8DecryptorProviderBuilder providerBuilder = new JceOpenSSLPKCS8DecryptorProviderBuilder();
            InputDecryptorProvider provider = providerBuilder.build(Strings.nullToEmpty(password).toCharArray());
            PrivateKeyInfo info = pInfo.decryptPrivateKeyInfo(provider);
            privateKey = converter.getPrivateKey(info);
        } else if (object instanceof PrivateKeyInfo) {
            privateKey = converter.getPrivateKey((PrivateKeyInfo) object);
        } else {
            throw new PKCSException("Encountered unexpected object type: " + object.getClass().getName());
        }

        return privateKey;
    }
}

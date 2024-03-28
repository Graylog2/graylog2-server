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

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.openssl.PEMKeyPair;
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
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;

public class KeyUtil {
    private static final Logger LOG = LoggerFactory.getLogger(KeyUtil.class);

    public static X509Certificate[] loadX509Certificates(Path certificatePath) throws KeyUtilException {
        try {
            return loadCertificates(certificatePath).stream()
                    .filter(certificate -> certificate instanceof X509Certificate)
                    .map(certificate -> (X509Certificate) certificate)
                    .toArray(X509Certificate[]::new);
        } catch (CertificateException | IOException | CMSException e) {
            throw new KeyUtilException(e);
        }
    }

    public static Collection<? extends Certificate> loadCertificates(Path certificatePath) throws CertificateException, IOException, CMSException {
        File certFile = certificatePath.toFile();

        if (certFile.isDirectory()) {
            final ImmutableList.Builder<X509Certificate> certs = ImmutableList.builder();
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(certFile.toPath())) {
                for (Path f : ds) {
                    certs.addAll(extractCertsFromPemFile(f));
                }
            }
            return certs.build();
        } else {
            return extractCertsFromPemFile(certificatePath);
        }
    }

    private static List<X509Certificate> extractCertsFromPemFile(Path pemFile) throws IOException, CMSException, CertificateException {

        final ImmutableList.Builder<X509Certificate> certs = ImmutableList.builder();

        final JcaX509CertificateConverter converter = new JcaX509CertificateConverter();
        try (InputStream inputStream = Files.newInputStream(pemFile);
             InputStreamReader fileReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             PEMParser pemParser = new PEMParser(fileReader)) {

            Object object;
            while ((object = pemParser.readObject()) != null) {
                if (object instanceof X509CertificateHolder) {
                    X509CertificateHolder certificateHolder = (X509CertificateHolder) object;
                    certs.add(converter.getCertificate(certificateHolder));

                } else if (object instanceof ContentInfo) {
                    // PKCS#7 Files
                    ContentInfo contentInfo = (ContentInfo) object;
                    final CMSSignedData cmsSignedData = new CMSSignedData(contentInfo);
                    for (X509CertificateHolder ch : cmsSignedData.getCertificates().getMatches(null)) {
                        certs.add(converter.getCertificate(ch));
                    }
                } else {
                    LOG.debug("Ignoring non certtifacte {} in {}", object.getClass().getCanonicalName(), pemFile);
                    // TODO remove
                    LOG.info("Ignoring non certtifacte {} in {}", object.getClass().getCanonicalName(), pemFile);
                }
            }
        }
        return certs.build();
    }

    /**
     * Build a password-encrypted PKCS8 private key and write it to a PEM file in the temp directory.
     * Caller is responsible for ensuring that the temp directory is writable. The file will be deleted
     * when the VM exits.
     * @param tmpDir path to directory in which to create the
     * @param password to protect the key
     * @param key encrypt this key
     * @return PEM file
     * @throws GeneralSecurityException
     */
    public static File generatePKCS8FromPrivateKey(Path tmpDir, char[] password, PrivateKey key) throws GeneralSecurityException {
        try {
            JceOpenSSLPKCS8EncryptorBuilder encryptorBuilder =
                    new JceOpenSSLPKCS8EncryptorBuilder(PKCS8Generator.AES_256_CBC)
                            .setRandom(new SecureRandom())
                            .setPassword(password);
            OutputEncryptor encryptor = encryptorBuilder.build();

            // construct object to create the PKCS8 object from the private key and encryptor
            PemObject pemObj = new JcaPKCS8Generator(key, encryptor).generate();
            StringWriter stringWriter = new StringWriter();
            try (JcaPEMWriter pemWriter = new JcaPEMWriter(stringWriter)) {
                pemWriter.writeObject(pemObj);
            }

            // write PKCS8 to file
            String pkcs8Key = stringWriter.toString();
            File tmpFile = Files.createTempFile(tmpDir, "pkcs8", ".key").toFile();
            try (FileOutputStream fos = new FileOutputStream(tmpFile)) {
                fos.write(pkcs8Key.getBytes(StandardCharsets.UTF_8));
                tmpFile.deleteOnExit();
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
    public static PrivateKey privateKeyFromFile(String password, File keyFile) throws KeyUtilException {
        try {
            return getPrivateKeyFromFile(password, keyFile);
        } catch (IOException | PKCSException | OperatorCreationException e) {
            throw new KeyUtilException(e);
        }
    }

    private static PrivateKey getPrivateKeyFromFile(String password, File keyFile) throws IOException, PKCSException, OperatorCreationException {
        PrivateKey privateKey;

        JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");

        // Be sure to specify charset for reader - don't use plain FileReader
        Object object;
        try (InputStream inputStream = Files.newInputStream(keyFile.toPath());
             InputStreamReader fileReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             PEMParser pemParser = new PEMParser(fileReader)) {
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
        } else if (object instanceof PEMKeyPair) {
            privateKey = converter.getPrivateKey(((PEMKeyPair) object).getPrivateKeyInfo());
        } else if (object == null) {
            throw new PKCSException("No key found in PEM file <" + keyFile + ">");
        } else {
            throw new PKCSException("Encountered unexpected object type: " + object.getClass().getName());
        }

        return privateKey;
    }
}

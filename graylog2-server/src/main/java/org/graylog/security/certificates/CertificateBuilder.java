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
package org.graylog.security.certificates;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.graylog2.security.encryption.EncryptedValueService;

import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Builder for creating X.509 certificates with BouncyCastle.
 * <p>
 * This class provides methods for generating key pairs and creating certificates
 * (root CAs, intermediate CAs, and end-entity certificates) using Ed25519 or RSA algorithms.
 * <p>
 * TODO: Consider consolidating with CertificateGenerator in the certutil package.
 *       That class handles RSA certificates for DataNode/TLS; this class adds Ed25519 support
 *       for OpAMP enrollment. A unified certificate builder could serve both use cases.
 */
public class CertificateBuilder {

    static {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    private final EncryptedValueService encryptedValueService;

    /**
     * Creates a new CertificateBuilder.
     *
     * @param encryptedValueService service for encrypting private keys before storage
     */
    public CertificateBuilder(EncryptedValueService encryptedValueService) {
        this.encryptedValueService = encryptedValueService;
    }

    /**
     * Generates a cryptographic key pair using the specified algorithm.
     * Uses the BouncyCastle provider for key generation.
     *
     * @param algorithm the algorithm to use for key generation
     * @return a newly generated key pair
     * @throws NoSuchAlgorithmException if the algorithm is not available
     * @throws NoSuchProviderException if the BouncyCastle provider is not available
     */
    public KeyPair generateKeyPair(Algorithm algorithm) throws NoSuchAlgorithmException, NoSuchProviderException {
        final KeyPairGenerator keyGen = KeyPairGenerator.getInstance(algorithm.keyAlgorithm(), "BC");
        if (algorithm == Algorithm.RSA_4096) {
            keyGen.initialize(algorithm.keySize(), new SecureRandom());
        }
        return keyGen.generateKeyPair();
    }

    /**
     * Creates a self-signed root CA certificate.
     * The certificate includes:
     * <ul>
     *   <li>Subject/Issuer: CN=commonName (self-signed)</li>
     *   <li>Basic Constraints: CA:TRUE (critical)</li>
     *   <li>Key Usage: keyCertSign | cRLSign (critical)</li>
     *   <li>Serial number: timestamp-based</li>
     * </ul>
     *
     * @param commonName the common name for the CA certificate
     * @param algorithm the algorithm to use for key generation and signing
     * @param validity the validity period of the certificate
     * @return a CertificateEntry with encrypted private key and PEM-encoded certificate (no ID - not yet saved)
     * @throws Exception if certificate creation fails
     */
    public CertificateEntry createRootCa(String commonName, Algorithm algorithm, Duration validity) throws Exception {
        final KeyPair keyPair = generateKeyPair(algorithm);

        final X500Name subject = new X500NameBuilder(BCStyle.INSTANCE)
                .addRDN(BCStyle.CN, commonName)
                .build();

        final Instant now = Instant.now();
        final Instant notAfter = now.plus(validity);
        final BigInteger serialNumber = BigInteger.valueOf(System.currentTimeMillis());

        final JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                subject,
                serialNumber,
                Date.from(now),
                Date.from(notAfter),
                subject,
                keyPair.getPublic()
        );

        // Add Basic Constraints: CA:TRUE (critical)
        certBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));

        // Add Key Usage: keyCertSign | cRLSign (critical)
        certBuilder.addExtension(
                Extension.keyUsage,
                true,
                new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign)
        );

        final X509Certificate certificate = signCertificate(certBuilder, keyPair.getPrivate(), algorithm);

        return buildEntry(certificate, keyPair.getPrivate(), List.of());
    }

    /**
     * Creates an intermediate CA certificate signed by the specified issuer.
     * The certificate includes:
     * <ul>
     *   <li>Subject: CN=commonName</li>
     *   <li>Issuer: from parent CA certificate</li>
     *   <li>Basic Constraints: CA:TRUE with pathLen:0 (critical)</li>
     *   <li>Key Usage: keyCertSign (critical)</li>
     *   <li>Algorithm: derived from issuer certificate</li>
     * </ul>
     * <p>
     * The pathLen:0 constraint means this intermediate CA can only sign end-entity
     * certificates, not sub-CAs.
     *
     * @param commonName the common name for the intermediate CA certificate
     * @param issuer the issuing CA's certificate entry (must contain the private key)
     * @param validity the validity period of the certificate
     * @return a CertificateEntry with encrypted private key and PEM-encoded certificate (no ID - not yet saved)
     * @throws Exception if certificate creation fails
     */
    public CertificateEntry createIntermediateCa(String commonName, CertificateEntry issuer, Duration validity) throws Exception {
        // Parse the issuer's certificate and private key
        final X509Certificate issuerCert = PemUtils.parseCertificate(issuer.certificate());
        final PrivateKey issuerPrivateKey = PemUtils.parsePrivateKey(
                encryptedValueService.decrypt(issuer.privateKey())
        );

        // Detect algorithm from issuer certificate
        final Algorithm algorithm = PemUtils.detectAlgorithm(issuerCert);

        // Generate key pair for the new intermediate CA
        final KeyPair keyPair = generateKeyPair(algorithm);

        // Build the subject DN
        final X500Name subject = new X500NameBuilder(BCStyle.INSTANCE)
                .addRDN(BCStyle.CN, commonName)
                .build();

        // Get the issuer DN from the issuer certificate
        final X500Name issuerDn = new X500Name(issuerCert.getSubjectX500Principal().getName());

        final Instant now = Instant.now();
        final Instant notAfter = now.plus(validity);
        final BigInteger serialNumber = BigInteger.valueOf(System.currentTimeMillis());

        final JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                issuerDn,
                serialNumber,
                Date.from(now),
                Date.from(notAfter),
                subject,
                keyPair.getPublic()
        );

        // Add Basic Constraints: CA:TRUE with pathLen:0 (critical)
        // pathLen:0 means this CA can only sign end-entity certs, not sub-CAs
        certBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(0));

        // Add Key Usage: keyCertSign (critical)
        certBuilder.addExtension(
                Extension.keyUsage,
                true,
                new KeyUsage(KeyUsage.keyCertSign)
        );

        final X509Certificate certificate = signCertificate(certBuilder, issuerPrivateKey, algorithm);

        // Build the issuer chain: issuer's certificate + issuer's chain
        final List<String> issuerChain = new ArrayList<>();
        issuerChain.add(issuer.certificate());
        issuerChain.addAll(issuer.issuerChain());

        return buildEntry(certificate, keyPair.getPrivate(), issuerChain);
    }

    /**
     * Creates an end-entity certificate signed by the specified issuer.
     * The certificate includes:
     * <ul>
     *   <li>Subject: CN=commonName</li>
     *   <li>Issuer: from parent CA certificate</li>
     *   <li>Basic Constraints: CA:FALSE (critical)</li>
     *   <li>Key Usage: configurable via keyUsageBits parameter (critical)</li>
     *   <li>Algorithm: derived from issuer certificate</li>
     * </ul>
     * <p>
     * End-entity certificates cannot issue other certificates. Use this for
     * certificates like token signing keys.
     *
     * @param commonName the common name for the end-entity certificate
     * @param issuer the issuing CA's certificate entry (must contain the private key)
     * @param keyUsageBits the key usage bits (e.g., {@link KeyUsage#digitalSignature})
     * @param validity the validity period of the certificate
     * @return a CertificateEntry with encrypted private key and PEM-encoded certificate (no ID - not yet saved)
     * @throws Exception if certificate creation fails
     */
    public CertificateEntry createEndEntityCert(String commonName, CertificateEntry issuer, int keyUsageBits, Duration validity) throws Exception {
        // Parse the issuer's certificate and private key
        final X509Certificate issuerCert = PemUtils.parseCertificate(issuer.certificate());
        final PrivateKey issuerPrivateKey = PemUtils.parsePrivateKey(
                encryptedValueService.decrypt(issuer.privateKey())
        );

        // Detect algorithm from issuer certificate
        final Algorithm algorithm = PemUtils.detectAlgorithm(issuerCert);

        // Generate key pair for the new end-entity certificate
        final KeyPair keyPair = generateKeyPair(algorithm);

        // Build the subject DN
        final X500Name subject = new X500NameBuilder(BCStyle.INSTANCE)
                .addRDN(BCStyle.CN, commonName)
                .build();

        // Get the issuer DN from the issuer certificate
        final X500Name issuerDn = new X500Name(issuerCert.getSubjectX500Principal().getName());

        final Instant now = Instant.now();
        final Instant notAfter = now.plus(validity);
        final BigInteger serialNumber = BigInteger.valueOf(System.currentTimeMillis());

        final JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                issuerDn,
                serialNumber,
                Date.from(now),
                Date.from(notAfter),
                subject,
                keyPair.getPublic()
        );

        // Add Basic Constraints: CA:FALSE (critical)
        certBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));

        // Add Key Usage: configurable via keyUsageBits (critical)
        certBuilder.addExtension(
                Extension.keyUsage,
                true,
                new KeyUsage(keyUsageBits)
        );

        final X509Certificate certificate = signCertificate(certBuilder, issuerPrivateKey, algorithm);

        // Build the issuer chain: issuer's certificate + issuer's chain
        final List<String> issuerChain = new ArrayList<>();
        issuerChain.add(issuer.certificate());
        issuerChain.addAll(issuer.issuerChain());

        return buildEntry(certificate, keyPair.getPrivate(), issuerChain);
    }

    private X509Certificate signCertificate(JcaX509v3CertificateBuilder certBuilder, PrivateKey signingKey, Algorithm algorithm)
            throws OperatorCreationException, CertificateException {
        final ContentSigner signer = new JcaContentSignerBuilder(algorithm.signatureAlgorithm())
                .setProvider("BC")
                .build(signingKey);

        final X509CertificateHolder certHolder = certBuilder.build(signer);
        return new JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(certHolder);
    }

    private CertificateEntry buildEntry(X509Certificate certificate, PrivateKey privateKey, List<String> issuerChain)
            throws Exception {
        final String fingerprint = PemUtils.computeFingerprint(certificate);
        final String certificatePem = PemUtils.toPem(certificate);
        final String privateKeyPem = PemUtils.toPem(privateKey);

        return new CertificateEntry(
                null, // ID assigned on save
                fingerprint,
                encryptedValueService.encrypt(privateKeyPem),
                certificatePem,
                issuerChain,
                certificate.getNotBefore().toInstant(),
                certificate.getNotAfter().toInstant(),
                Instant.now()
        );
    }
}

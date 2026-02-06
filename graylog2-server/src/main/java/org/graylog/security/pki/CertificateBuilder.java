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
package org.graylog.security.pki;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.ContentVerifierProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCSException;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.graylog2.security.encryption.EncryptedValueService;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.graylog2.shared.utilities.StringUtils.f;

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
                null, // subjectDn - extracted on save
                null, // issuerDn - extracted on save
                certificate.getNotBefore().toInstant(),
                certificate.getNotAfter().toInstant(),
                Instant.now()
        );
    }

    /**
     * Creates a Certificate Signing Request (CSR) for a given key pair and common name.
     * <p>
     * This method is primarily used for testing; in production, agents generate their own CSRs.
     *
     * @param keyPair the key pair to create the CSR for
     * @param commonName the common name to include in the CSR subject
     * @return the PEM-encoded CSR as a byte array
     * @throws IOException if encoding fails
     * @throws OperatorCreationException if the content signer cannot be created
     */
    public byte[] createCsr(KeyPair keyPair, String commonName) throws IOException, OperatorCreationException {
        final X500Name subject = new X500NameBuilder(BCStyle.INSTANCE)
                .addRDN(BCStyle.CN, commonName)
                .build();

        // Detect algorithm from key pair
        final String keyAlgorithm = keyPair.getPublic().getAlgorithm();
        final String signatureAlgorithm = "Ed25519".equals(keyAlgorithm) ? "Ed25519" : "SHA256withRSA";

        final ContentSigner signer = new JcaContentSignerBuilder(signatureAlgorithm)
                .setProvider("BC")
                .build(keyPair.getPrivate());

        final PKCS10CertificationRequest csr = new JcaPKCS10CertificationRequestBuilder(subject, keyPair.getPublic())
                .build(signer);

        // Encode CSR as PEM
        final StringWriter stringWriter = new StringWriter();
        try (JcaPEMWriter pemWriter = new JcaPEMWriter(stringWriter)) {
            pemWriter.writeObject(csr);
        }
        return stringWriter.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Signs a Certificate Signing Request (CSR) and produces an X.509 certificate.
     * <p>
     * This method:
     * <ul>
     *   <li>Parses and verifies the CSR self-signature (proves agent has private key)</li>
     *   <li>Validates that the public key is Ed25519</li>
     *   <li>Ignores the CSR subject - uses the provided subjectCn instead</li>
     *   <li>Sets extensions: BasicConstraints CA:FALSE (critical), KeyUsage digitalSignature (critical),
     *       ExtendedKeyUsage clientAuth</li>
     * </ul>
     *
     * @param csrPem the PEM-encoded CSR
     * @param issuer the issuing CA's certificate entry (must contain the private key)
     * @param subjectCn the common name for the certificate subject (CSR subject is ignored)
     * @param validity the validity period of the certificate
     * @return the signed X509Certificate
     * @throws Exception if signing fails
     * @throws IllegalArgumentException if the CSR public key is not Ed25519
     */
    public X509Certificate signCsr(byte[] csrPem, CertificateEntry issuer, String subjectCn, Duration validity)
            throws Exception {
        // Parse the CSR
        final PKCS10CertificationRequest csr;
        try (PEMParser pemParser = new PEMParser(new StringReader(new String(csrPem, StandardCharsets.UTF_8)))) {
            final Object object = pemParser.readObject();
            if (!(object instanceof PKCS10CertificationRequest)) {
                throw new IllegalArgumentException("PEM does not contain a valid CSR");
            }
            csr = (PKCS10CertificationRequest) object;
        }

        // Verify CSR self-signature (proves agent possesses private key)
        try {
            final ContentVerifierProvider verifier = new JcaContentVerifierProviderBuilder()
                    .setProvider("BC")
                    .build(csr.getSubjectPublicKeyInfo());
            if (!csr.isSignatureValid(verifier)) {
                throw new IllegalArgumentException("CSR signature verification failed");
            }
        } catch (PKCSException e) {
            throw new IllegalArgumentException("CSR signature verification failed", e);
        }

        // Extract and validate public key - must be Ed25519
        final PublicKey publicKey = new org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter()
                .setProvider("BC")
                .getPublicKey(csr.getSubjectPublicKeyInfo());

        if (!"Ed25519".equals(publicKey.getAlgorithm())) {
            throw new IllegalArgumentException(
                    f("CSR public key must be Ed25519, but was %s", publicKey.getAlgorithm())
            );
        }

        // Parse the issuer's certificate and private key
        final X509Certificate issuerCert = PemUtils.parseCertificate(issuer.certificate());
        final PrivateKey issuerPrivateKey = PemUtils.parsePrivateKey(
                encryptedValueService.decrypt(issuer.privateKey())
        );

        // Build the subject DN (ignore CSR subject, use provided subjectCn)
        final X500Name subject = new X500NameBuilder(BCStyle.INSTANCE)
                .addRDN(BCStyle.CN, subjectCn)
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
                publicKey
        );

        // Add Basic Constraints: CA:FALSE (critical)
        certBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));

        // Add Key Usage: digitalSignature (critical)
        certBuilder.addExtension(
                Extension.keyUsage,
                true,
                new KeyUsage(KeyUsage.digitalSignature)
        );

        // Add Extended Key Usage: clientAuth
        certBuilder.addExtension(
                Extension.extendedKeyUsage,
                false,
                new ExtendedKeyUsage(KeyPurposeId.id_kp_clientAuth)
        );

        // Detect algorithm from issuer certificate
        final Algorithm algorithm = PemUtils.detectAlgorithm(issuerCert);

        return signCertificate(certBuilder, issuerPrivateKey, algorithm);
    }
}

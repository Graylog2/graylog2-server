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
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Utility class for PEM encoding, decoding, and certificate operations.
 * <p>
 * This class provides static methods for working with X.509 certificates and private keys
 * in PEM format, as well as computing certificate fingerprints.
 * <p>
 * TODO: Consider consolidating with SAMLCredentialUtils (enterprise plugin) and
 *       PemCaReader/PemReader (certutil package) to reduce code duplication across
 *       the codebase. All use the same BouncyCastle patterns for PEM handling.
 */
public final class PemUtils {

    static {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    private PemUtils() {
        // Utility class - no instantiation
    }

    /**
     * Encodes an X.509 certificate as PEM.
     *
     * @param certificate the certificate to encode
     * @return the PEM-encoded certificate
     * @throws IOException if encoding fails
     */
    public static String toPem(X509Certificate certificate) throws IOException {
        final StringWriter writer = new StringWriter();
        try (JcaPEMWriter pemWriter = new JcaPEMWriter(writer)) {
            pemWriter.writeObject(certificate);
        }
        return writer.toString();
    }

    /**
     * Encodes a private key as PEM.
     *
     * @param privateKey the private key to encode
     * @return the PEM-encoded private key
     * @throws IOException if encoding fails
     */
    public static String toPem(PrivateKey privateKey) throws IOException {
        final StringWriter writer = new StringWriter();
        try (JcaPEMWriter pemWriter = new JcaPEMWriter(writer)) {
            pemWriter.writeObject(privateKey);
        }
        return writer.toString();
    }

    /**
     * Parses a PEM-encoded X.509 certificate.
     *
     * @param pem the PEM-encoded certificate
     * @return the parsed X509Certificate
     * @throws IOException if parsing fails
     * @throws CertificateException if the certificate is invalid
     */
    public static X509Certificate parseCertificate(String pem) throws IOException, CertificateException {
        try (PEMParser pemParser = new PEMParser(new StringReader(pem))) {
            final Object object = pemParser.readObject();
            if (object instanceof X509CertificateHolder holder) {
                return new JcaX509CertificateConverter().getCertificate(holder);
            }
            throw new CertificateException("PEM does not contain a valid X.509 certificate");
        }
    }

    /**
     * Parses a PEM-encoded private key.
     * Supports both PKCS#8 (PrivateKeyInfo) and traditional (PEMKeyPair) formats.
     *
     * @param pem the PEM-encoded private key
     * @return the parsed PrivateKey
     * @throws IOException if parsing fails or the PEM doesn't contain a valid private key
     */
    public static PrivateKey parsePrivateKey(String pem) throws IOException {
        try (PEMParser pemParser = new PEMParser(new StringReader(pem))) {
            final Object object = pemParser.readObject();
            final JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            if (object instanceof org.bouncycastle.asn1.pkcs.PrivateKeyInfo privateKeyInfo) {
                return converter.getPrivateKey(privateKeyInfo);
            } else if (object instanceof PEMKeyPair pemKeyPair) {
                return converter.getPrivateKey(pemKeyPair.getPrivateKeyInfo());
            }
            throw new IOException("PEM does not contain a valid private key");
        }
    }

    /**
     * Computes the SHA-256 fingerprint of an X.509 certificate.
     *
     * @param certificate the certificate to compute the fingerprint for
     * @return the fingerprint in format "sha256:hexstring"
     * @throws CertificateEncodingException if the certificate cannot be encoded
     * @throws NoSuchAlgorithmException if SHA-256 is not available
     */
    public static String computeFingerprint(X509Certificate certificate)
            throws CertificateEncodingException, NoSuchAlgorithmException {
        final MessageDigest digest = MessageDigest.getInstance("SHA-256");
        final byte[] hash = digest.digest(certificate.getEncoded());
        return "sha256:" + HexFormat.of().formatHex(hash);
    }

    /**
     * Converts our internal fingerprint format to RFC 7515 x5t#S256 format.
     * <p>
     * Our format: "sha256:hexstring" (e.g., "sha256:abc123...")
     * x5t#S256 format: base64url-encoded raw SHA-256 bytes
     *
     * @param fingerprint our internal fingerprint format
     * @return base64url-encoded thumbprint for use in JWT x5t#S256 header
     */
    public static String fingerprintToX5t(String fingerprint) {
        if (!fingerprint.startsWith("sha256:")) {
            throw new IllegalArgumentException("Fingerprint must start with 'sha256:'");
        }
        final String hex = fingerprint.substring("sha256:".length());
        final byte[] bytes = HexFormat.of().parseHex(hex);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Converts RFC 7515 x5t#S256 format to our internal fingerprint format.
     * <p>
     * x5t#S256 format: base64url-encoded raw SHA-256 bytes
     * Our format: "sha256:hexstring" (e.g., "sha256:abc123...")
     *
     * @param x5t base64url-encoded thumbprint from JWT x5t#S256 header
     * @return our internal fingerprint format
     */
    public static String x5tToFingerprint(String x5t) {
        final byte[] bytes = Base64.getUrlDecoder().decode(x5t);
        return "sha256:" + HexFormat.of().formatHex(bytes);
    }

    /**
     * Extracts the Common Name (CN) from an X.509 certificate's subject distinguished name.
     *
     * @param certificate the certificate to extract the CN from
     * @return the CN value as a string
     * @throws IllegalArgumentException if the certificate has no CN in its subject DN
     */
    public static String extractCn(X509Certificate certificate) {
        final X500Name x500 = new X500Name(certificate.getSubjectX500Principal().getName());
        final var rdns = x500.getRDNs(BCStyle.CN);
        if (rdns.length == 0) {
            throw new IllegalArgumentException("Certificate subject has no CN");
        }
        return rdns[0].getFirst().getValue().toString();
    }

    /**
     * Detects the algorithm used by a certificate based on its public key.
     *
     * @param certificate the certificate to analyze
     * @return the detected Algorithm
     * @throws IllegalArgumentException if the algorithm is not supported
     */
    public static Algorithm detectAlgorithm(X509Certificate certificate) {
        final String keyAlgorithm = certificate.getPublicKey().getAlgorithm();
        return switch (keyAlgorithm) {
            case "Ed25519", "EdDSA" -> Algorithm.ED25519;
            case "RSA" -> Algorithm.RSA_4096;
            default -> throw new IllegalArgumentException("Unsupported key algorithm: " + keyAlgorithm);
        };
    }
}

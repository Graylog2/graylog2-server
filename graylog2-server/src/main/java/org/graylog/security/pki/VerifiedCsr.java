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

import org.bouncycastle.openssl.PEMException;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.operator.ContentVerifierProvider;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import java.security.PublicKey;
import java.security.Security;
import java.security.cert.CertificateException;

/**
 * A PKCS#10 Certificate Signing Request whose self-signature has been verified.
 * <p>
 * A valid self-signature proves that the requester holds the private key corresponding to the
 * public key carried in the CSR (proof-of-possession). Instances can only be obtained through
 * {@link PemUtils#parseCsr(String)} or the package-private {@link #verify} factory, both of which
 * perform that verification — so holding a {@code VerifiedCsr} is a compile-time guarantee that
 * the check happened. APIs that must not operate on unverified CSRs, such as
 * {@link CertificateBuilder#signCsr}, accept this type instead of a raw
 * {@link PKCS10CertificationRequest} to make the verification requirement impossible to skip.
 */
public final class VerifiedCsr {

    static {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    private final PKCS10CertificationRequest csr;

    private VerifiedCsr(PKCS10CertificationRequest csr) {
        this.csr = csr;
    }

    /**
     * Verifies the CSR's self-signature and wraps it.
     * <p>
     * This factory is algorithm-agnostic — policy such as "CSRs must use Ed25519" is enforced by
     * higher-level callers (e.g., {@link CertificateBuilder#signCsr}), not here.
     *
     * @param csr the parsed CSR
     * @return the signature-verified CSR
     * @throws CertificateException if the CSR's self-signature cannot be verified
     */
    static VerifiedCsr verify(PKCS10CertificationRequest csr) throws CertificateException {
        final boolean signatureValid;
        try {
            final ContentVerifierProvider verifier = new JcaContentVerifierProviderBuilder()
                    .setProvider("BC")
                    .build(csr.getSubjectPublicKeyInfo());
            signatureValid = csr.isSignatureValid(verifier);
        } catch (Exception e) {
            throw new CertificateException("CSR signature verification failed", e);
        }

        if (!signatureValid) {
            throw new CertificateException("CSR signature is invalid");
        }

        return new VerifiedCsr(csr);
    }

    /**
     * @return the underlying PKCS#10 CSR
     */
    public PKCS10CertificationRequest csr() {
        return csr;
    }

    /**
     * Extracts the public key from the CSR. No algorithm policy is enforced; callers that require
     * a specific key algorithm must check {@link PublicKey#getAlgorithm()} on the returned key
     * themselves.
     *
     * @return the public key contained in the CSR
     * @throws PEMException if the CSR's {@code SubjectPublicKeyInfo} cannot be converted to a
     *                      {@link PublicKey} (e.g., unsupported key algorithm)
     */
    public PublicKey publicKey() throws PEMException {
        return new JcaPEMKeyConverter()
                .setProvider("BC")
                .getPublicKey(csr.getSubjectPublicKeyInfo());
    }
}

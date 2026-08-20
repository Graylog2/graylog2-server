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
package org.graylog.collectors.opamp;

import org.graylog.security.pki.CertificateEntry;
import org.graylog.security.pki.PemUtils;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.time.Instant;

/**
 * Bundle of fields derived from a freshly signed agent certificate.
 *
 * @param fingerprint the SHA-256 fingerprint of the certificate (see {@link PemUtils#computeFingerprint})
 * @param certPem     the PEM-encoded certificate
 * @param notAfter    the certificate's expiration instant
 * @param issuerId    the database id of the issuing CA (see {@link CertificateEntry#id()})
 */
public record IssuedCertificate(String fingerprint, String certPem, Instant notAfter, String issuerId) {
    /**
     * Builds an {@link IssuedCertificate} from a signed X.509 certificate and its issuer entry.
     *
     * @param cert   the signed agent certificate
     * @param issuer the CA entry whose private key signed {@code cert}
     * @return the populated bundle
     * @throws CertificateEncodingException if the certificate cannot be DER-encoded
     * @throws NoSuchAlgorithmException     if SHA-256 is unavailable
     * @throws IOException                  if the certificate cannot be PEM-encoded
     */
    public static IssuedCertificate of(X509Certificate cert, CertificateEntry issuer)
            throws CertificateEncodingException, NoSuchAlgorithmException, IOException {
        return new IssuedCertificate(
                PemUtils.computeFingerprint(cert),
                PemUtils.toPem(cert),
                cert.getNotAfter().toInstant(),
                issuer.id()
        );
    }
}

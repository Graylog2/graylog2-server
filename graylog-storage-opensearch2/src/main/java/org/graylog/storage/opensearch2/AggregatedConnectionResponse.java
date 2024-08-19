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
package org.graylog.storage.opensearch2;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMParser;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.StringReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public record AggregatedConnectionResponse(Map<String, ConnectionCheckResponse> responses) {
    @Nonnull
    public List<ConnectionCheckIndex> indices() {
        return responses.values().stream()
                .filter(v -> Objects.nonNull(v.indices()))
                .flatMap(v -> v.indices().stream())
                .sorted(Comparator.comparing(ConnectionCheckIndex::name, Comparator.naturalOrder()))
                .distinct()
                .collect(Collectors.toList());
    }

    @Nullable
    public String error() {
        final String errorMessage = responses.entrySet().stream()
                .filter(e -> Objects.nonNull(e.getValue().error()))
                .map(e -> e.getKey() + ": " + e.getValue().error())
                .collect(Collectors.joining(";"));

        if (errorMessage.isEmpty()) {
            return null;
        }

        final StringBuilder errorBuilder = new StringBuilder();
        errorBuilder.append(errorMessage);

        if (!certificates().isEmpty()) {
            errorBuilder.append("\n").append("Unknown certificates: \n").append(certificates().stream().map(AggregatedConnectionResponse::decode).map(this::info).collect(Collectors.joining("\n\n")));
        }
        return errorBuilder.toString();
    }

    private String info(X509Certificate certificate) {
        return """
                Issued to: %s,
                Issued by: %s,
                Serial number: %s,
                Issued on: %s,
                Expires on: %s,
                SHA-256 fingerprint: %s,
                SHA-1 Fingerprint: %s
                """.formatted(
                certificate.getSubjectX500Principal().getName(),
                certificate.getIssuerX500Principal().getName(),
                certificate.getSerialNumber(),
                certificate.getNotBefore(),
                certificate.getNotAfter(),
                getfingerprint(certificate, "SHA-256"),
                getfingerprint(certificate, "SHA-1")
        );
    }

    @Nonnull
    public List<String> certificates() {
        return responses.values().stream()
                .filter(v -> Objects.nonNull(v.certificates()))
                .flatMap(v -> v.certificates().stream())
                .distinct()
                .collect(Collectors.toList());
    }

    private static X509Certificate decode(String pemEncodedCert) {
        final PEMParser pemParser = new PEMParser(new StringReader(pemEncodedCert));
        try {
            Object parsed = pemParser.readObject();
            if (parsed instanceof X509CertificateHolder certificate) {
                return new JcaX509CertificateConverter().getCertificate(certificate);
            } else {
                throw new IllegalArgumentException("Couldn't parse x509 certificate from provided string, unknown type");
            }
        } catch (IOException | CertificateException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getfingerprint(X509Certificate cert, String type) {
        try {
            MessageDigest md = MessageDigest.getInstance(type);
            byte[] der = cert.getEncoded();
            md.update(der);
            byte[] digest = md.digest();
            String digestHex = DatatypeConverter.printHexBinary(digest);
            return digestHex.toLowerCase(Locale.ROOT);
        } catch (CertificateEncodingException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}

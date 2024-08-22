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
package org.graylog2.datanode;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMParser;

import java.io.IOException;
import java.io.StringReader;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public record RemoteReindexAllowlistEvent(List<String> allowlist, ACTION action,
                                          List<String> pemEncodedTrustedCertificates) {

    public enum ACTION {
        ADD, REMOVE
    }

    public static RemoteReindexAllowlistEvent add(List<String> allowlist) {
        return add(allowlist, Collections.emptyList());
    }

    /**
     * @param allowlist
     * @param trustedCertificates PEM encoded certificates that the datanodes should trust
     * @return
     */
    public static RemoteReindexAllowlistEvent add(List<String> allowlist, List<String> trustedCertificates) {
        return new RemoteReindexAllowlistEvent(allowlist, ACTION.ADD, trustedCertificates);
    }

    public static RemoteReindexAllowlistEvent remove() {
        return new RemoteReindexAllowlistEvent(Collections.emptyList(), ACTION.REMOVE, Collections.emptyList());
    }

    @JsonIgnore
    public List<X509Certificate> trustedCertificates() {
        return pemEncodedTrustedCertificates.stream().map(RemoteReindexAllowlistEvent::decode).collect(Collectors.toList());
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
}

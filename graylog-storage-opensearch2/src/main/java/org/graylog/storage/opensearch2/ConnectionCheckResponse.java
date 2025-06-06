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

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.annotation.Nonnull;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

import java.io.IOException;
import java.io.StringWriter;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ConnectionCheckResponse(List<ConnectionCheckIndex> indices, List<String> certificates,
                                      String error) {
    public static ConnectionCheckResponse success(List<ConnectionCheckIndex> indices, List<X509Certificate> certificates) {
        return new ConnectionCheckResponse(indices, encodeCerts(certificates), null);
    }

    public static ConnectionCheckResponse error(Exception e, List<X509Certificate> certificates) {
        return new ConnectionCheckResponse(Collections.emptyList(), encodeCerts(certificates), e.getMessage());
    }

    @Nonnull
    private static List<String> encodeCerts(List<X509Certificate> trustedCertificates) {
        return Optional.ofNullable(trustedCertificates)
                .map(certs -> certs.stream().map(ConnectionCheckResponse::encode).collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }


    private static String encode(X509Certificate certificate) {
        var writer = new StringWriter();
        try (JcaPEMWriter jcaPEMWriter = new JcaPEMWriter(writer)) {
            jcaPEMWriter.writeObject(certificate);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return writer.toString();
    }
}

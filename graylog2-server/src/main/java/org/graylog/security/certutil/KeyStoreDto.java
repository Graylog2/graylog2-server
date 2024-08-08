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
package org.graylog.security.certutil;

import org.jetbrains.annotations.NotNull;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record KeyStoreDto(Map<String, List<CertificateDto>> certificates) {
    public static KeyStoreDto empty() {
        return new KeyStoreDto(new HashMap<>());
    }

    public static @NotNull KeyStoreDto fromKeyStore(KeyStore keystore) throws KeyStoreException {
        Map<String, List<CertificateDto>> certificates = new HashMap<>();
        Enumeration<String> aliases = keystore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            Certificate[] certificateChain = keystore.getCertificateChain(alias);
            if (certificateChain != null && certificateChain.length > 0) {
                List<CertificateDto> certs = Arrays.stream(certificateChain).filter(certificate -> certificate instanceof X509Certificate).map(certificate -> new CertificateDto((X509Certificate) certificate)).toList();
                certificates.put(alias, certs);
            } else {
                Certificate certificate = keystore.getCertificate(alias);
                if (certificate instanceof X509Certificate) {
                    certificates.put(alias, List.of(new CertificateDto((X509Certificate) certificate)));
                }
            }
        }
        return new KeyStoreDto(certificates);
    }

}

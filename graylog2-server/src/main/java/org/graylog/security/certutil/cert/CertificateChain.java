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
package org.graylog.security.certutil.cert;

import jakarta.annotation.Nullable;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record CertificateChain(X509Certificate signedCertificate,
                               @Nullable List<X509Certificate> caCertificates) {

    public Certificate[] toCertificateChainArray() {
        List<Certificate> certificates = new ArrayList<>();
        certificates.add(signedCertificate);
        Optional.ofNullable(caCertificates).ifPresent(certificates::addAll);
        return certificates.toArray(new Certificate[0]);
    }
}

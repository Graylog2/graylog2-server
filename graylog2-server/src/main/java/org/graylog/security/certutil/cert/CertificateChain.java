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

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;

public record CertificateChain(X509Certificate signedCertificate,
                               List<X509Certificate> caCertificates) {

    public Certificate[] toCertificateChainArray() {
        if (caCertificates != null) {
            Certificate[] array = new Certificate[caCertificates.size() + 1];
            array[0] = signedCertificate;
            int index = 1;
            for (X509Certificate caCert : caCertificates) {
                array[index++] = caCert;
            }
            return array;
        } else {
            return new Certificate[]{signedCertificate};
        }
    }
}

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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.cert.X509Certificate;
import java.time.Duration;

class CertificateGeneratorTest {

    @Test
    void testDomainName() throws Exception {
        final KeyPair pair = selfSigned("www.graylog.org");
        final X509Certificate certificate = pair.certificate();
        final String cn = certificate.getSubjectX500Principal().getName();
        Assertions.assertThat(cn).isEqualTo("CN=www.graylog.org");
    }

    @Test
    void testEscaping() throws Exception {
        final KeyPair pair = selfSigned("Graylog, Inc.");
        final X509Certificate certificate = pair.certificate();
        final String cn = certificate.getSubjectX500Principal().getName();
        Assertions.assertThat(cn).isEqualTo("CN=Graylog\\, Inc.");
    }

    private static KeyPair selfSigned(String cname) throws Exception {
        final CertRequest req = CertRequest.selfSigned(cname).validity(Duration.ofDays(1));
        return CertificateGenerator.generate(req);
    }
}

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

import org.assertj.core.api.Assertions;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.graylog.security.certutil.CertRequest;
import org.graylog.security.certutil.CertificateGenerator;
import org.graylog.security.certutil.KeyPair;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.List;

class RemoteReindexAllowlistEventTest {

    @Test
    void testEncodeDecodeCertificate() throws Exception {
        final KeyPair keyPair = CertificateGenerator.generate(CertRequest.selfSigned("my-server").validity(Duration.ofDays(31)));
        final X509Certificate certificate = keyPair.certificate();
        final RemoteReindexAllowlistEvent event = RemoteReindexAllowlistEvent.add(List.of("localhost:9200"), List.of(encodeAsPem(certificate)));

        final List<X509Certificate> certificates = event.trustedCertificates();
        Assertions.assertThat(certificates)
                .hasSize(1)
                .contains(certificate);

    }

    private String encodeAsPem(X509Certificate certificate) {
        var writer = new StringWriter();
        try (JcaPEMWriter jcaPEMWriter = new JcaPEMWriter(writer)) {
            jcaPEMWriter.writeObject(certificate);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return writer.toString();
    }
}

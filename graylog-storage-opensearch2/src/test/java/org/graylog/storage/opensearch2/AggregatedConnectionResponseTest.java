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
import java.util.Collections;
import java.util.List;
import java.util.Map;

class AggregatedConnectionResponseTest {

    @Test
    void testAggregateIndices() {
        final AggregatedConnectionResponse aggregatedResponse = new AggregatedConnectionResponse(Map.of(
                "node-one", new ConnectionCheckResponse(List.of(i("graylog_0"), i("graylog_1"), i("graylog_2")), Collections.emptyList(), null),
                "node-two", new ConnectionCheckResponse(List.of(i("graylog_0"), i("graylog_1"), i("graylog_2")), Collections.emptyList(), null),
                "node-three", new ConnectionCheckResponse(List.of(i("graylog_0"), i("graylog_1"), i("graylog_2")), Collections.emptyList(), null)
        ));
        Assertions.assertThat(aggregatedResponse.indices())
                .hasSize(3)
                .contains(i("graylog_0"), i("graylog_1"), i("graylog_2"));
    }

    private ConnectionCheckIndex i(String indexName) {
        return new ConnectionCheckIndex(indexName, false);
    }

    @Test
    void testAggregateErrors() {
        final AggregatedConnectionResponse aggregatedResponse = new AggregatedConnectionResponse(Map.of(
                "node-one", new ConnectionCheckResponse(null, null, "Failed to connect"),
                "node-two", new ConnectionCheckResponse(null, null, "Failed to connect"),
                "node-three", new ConnectionCheckResponse(null, null, "Failed to connect")
        ));
        Assertions.assertThat(aggregatedResponse.error())
                .contains("node-one: Failed to connect")
                .contains("node-two: Failed to connect")
                .contains("node-three: Failed to connect");

        Assertions.assertThat(aggregatedResponse.certificates())
                .isNotNull()
                .hasSize(0);

        Assertions.assertThat(aggregatedResponse.indices())
                .isNotNull()
                .hasSize(0);

    }

    @Test
    void testAggregateCertificates() throws Exception {
        final KeyPair keyPair = CertificateGenerator.generate(CertRequest.selfSigned("my-host").validity(Duration.ofDays(30)));
        final X509Certificate certificate = keyPair.certificate();
        final String pemCert = serializeAsPEM(certificate);


        final AggregatedConnectionResponse aggregatedResponse = new AggregatedConnectionResponse(Map.of(
                "node-one", new ConnectionCheckResponse(List.of(i("graylog_0"), i("graylog_1"), i("graylog_2")), Collections.singletonList(pemCert), null),
                "node-two", new ConnectionCheckResponse(List.of(i("graylog_0"), i("graylog_1"), i("graylog_2")), Collections.singletonList(pemCert), null),
                "node-three", new ConnectionCheckResponse(List.of(i("graylog_0"), i("graylog_1"), i("graylog_2")), Collections.singletonList(pemCert), null)
        ));

        Assertions.assertThat(aggregatedResponse.certificates())
                .hasSize(1)
                .contains(pemCert);

    }

    @Test
    void testAggregateCertificatesWithError() throws Exception {
        final KeyPair keyPair = CertificateGenerator.generate(CertRequest.selfSigned("my-host").validity(Duration.ofDays(30)));
        final X509Certificate certificate = keyPair.certificate();
        final String pemCert = serializeAsPEM(certificate);


        final AggregatedConnectionResponse aggregatedResponse = new AggregatedConnectionResponse(Map.of(
                "node-one", new ConnectionCheckResponse(List.of(i("graylog_0"), i("graylog_1"), i("graylog_2")), Collections.singletonList(pemCert), "Unknown cert"),
                "node-two", new ConnectionCheckResponse(List.of(i("graylog_0"), i("graylog_1"), i("graylog_2")), Collections.singletonList(pemCert), "Unknown cert"),
                "node-three", new ConnectionCheckResponse(List.of(i("graylog_0"), i("graylog_1"), i("graylog_2")), Collections.singletonList(pemCert), "Unknown cert")
        ));

        Assertions.assertThat(aggregatedResponse.error())
                .contains("node-one: Unknown cert")
                .contains("node-two: Unknown cert")
                .contains("node-three: Unknown cert")
                .contains("Unknown certificates:")
                .contains("Issued to: CN=my-host");

    }

    private String serializeAsPEM(final Object o) throws IOException {
        var writer = new StringWriter();
        try (JcaPEMWriter jcaPEMWriter = new JcaPEMWriter(writer)) {
            jcaPEMWriter.writeObject(o);
        }
        return writer.toString();
    }
}

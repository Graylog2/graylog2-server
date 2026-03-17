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

import opamp.proto.Opamp;
import org.graylog.collectors.config.TLSConfigurationSettings;
import org.graylog.collectors.config.exporter.OtlpExporterConfig;
import org.graylog.collectors.config.exporter.OtlpHttpExporterConfig;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for OTLP connection settings construction in {@link OpAmpService}.
 */
class OpAmpServiceOtlpSettingsTest {

    private static final String CA_PEM = "-----BEGIN CERTIFICATE-----\nfake-ca-pem\n-----END CERTIFICATE-----";
    private static final String CLUSTER_ID = "2209F727-F7E1-4123-9386-94FE3B354A07";

    private OtlpExporterConfig createExporterConfig() {
        return OtlpHttpExporterConfig.builder()
                .endpoint("https://otlp.example.com:14401")
                .tls(TLSConfigurationSettings.withCACert(CLUSTER_ID, CA_PEM))
                .build();
    }

    @Test
    void usesTlsCaPemContentsNotCertificateCaCert() {
        final var exporterConfig = createExporterConfig();

        final var builder = Opamp.ServerToAgent.newBuilder();
        OpAmpService.buildConnectionSettings(builder, exporterConfig);

        final var httpSettings = builder.getConnectionSettings().getOwnLogs();

        // Trust anchor must be in tls.ca_pem_contents
        assertThat(httpSettings.getTls().getCaPemContents()).isEqualTo(CA_PEM);
        // Must NOT be in certificate.ca_cert
        assertThat(httpSettings.getCertificate().getCaCert().toStringUtf8()).isEmpty();
    }

    @Test
    void setsCorrectDestinationEndpoint() {
        final var exporterConfig = createExporterConfig();

        final var builder = Opamp.ServerToAgent.newBuilder();
        OpAmpService.buildConnectionSettings(builder, exporterConfig);

        final var httpSettings = builder.getConnectionSettings().getOwnLogs();

        assertThat(httpSettings.getDestinationEndpoint())
                .isEqualTo("https://otlp.example.com:14401/?tls_server_name=" + CLUSTER_ID + "&log_level=info");
    }
}

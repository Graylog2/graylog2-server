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
package org.graylog.inputs.grpc;

import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.security.encryption.EncryptedValue;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AbstractGrpcTransportTest {
    @Test
    void insecureModeSkipsTlsValidation() {
        final Configuration config = config(Map.of(AbstractGrpcTransport.CK_INSECURE, true));

        assertThatCode(() -> AbstractGrpcTransport.checkTlsConfiguration(config))
                .doesNotThrowAnyException();
    }

    @Test
    void secureModeFailsWhenCertOrKeyIsMissing() {
        final Configuration config = config(Map.of(AbstractGrpcTransport.CK_INSECURE, false));

        assertThatThrownBy(() -> AbstractGrpcTransport.checkTlsConfiguration(config))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("TLS Server Certificate Chain")
                .hasMessageContaining("TLS Server Private Key");
    }

    @Test
    void secureModePassesWhenCertAndKeyAreSet() {
        final Map<String, Object> values = new HashMap<>();
        values.put(AbstractGrpcTransport.CK_INSECURE, false);
        values.put(AbstractGrpcTransport.CK_TLS_CERT, "base64-cert");
        values.put(AbstractGrpcTransport.CK_TLS_KEY, setEncryptedValue());

        assertThatCode(() -> AbstractGrpcTransport.checkTlsConfiguration(config(values)))
                .doesNotThrowAnyException();
    }

    @Test
    void insecureConnectionsAreAllowedByDefault() {
        final var insecureField = new AbstractGrpcTransport.Config()
                .getRequestedConfiguration()
                .getField(AbstractGrpcTransport.CK_INSECURE);

        assertThat(insecureField.getDefaultValue()).isEqualTo(true);
    }

    @Test
    void secureModeIsAssumedWhenInsecureKeyIsAbsent() {
        final Configuration config = config(Map.of());  // no CK_INSECURE key at all

        assertThatThrownBy(() -> AbstractGrpcTransport.checkTlsConfiguration(config))
                .isInstanceOf(ConfigurationException.class);
    }

    private static Configuration config(Map<String, Object> values) {
        return new Configuration(new HashMap<>(values));
    }

    private static EncryptedValue setEncryptedValue() {
        return EncryptedValue.builder()
                .value("encrypted-key")
                .salt("salt")
                .isKeepValue(false)
                .isDeleteValue(false)
                .build();
    }
}

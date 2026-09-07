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
package org.graylog2.utilities;

import com.github.joschi.jadconfig.ParameterException;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ProxyConfigConverterTest {

    @Test
    public void convertFromParsesAConfiguredProxy() {
        final ProxyConfigConverter converter = new ProxyConfigConverter();

        final ProxyConfig config = converter.convertFrom("http://user:pass@proxy.example.com:8123");

        assertThat(config.uri()).isEqualTo(URI.create("http://user:pass@proxy.example.com:8123"));
        assertThat(config.host()).isEqualTo("proxy.example.com");
        assertThat(config.port()).isEqualTo(8123);
    }

    @Test
    public void convertFromRejectsAMalformedUri() {
        final ProxyConfigConverter converter = new ProxyConfigConverter();

        assertThatThrownBy(() -> converter.convertFrom("://not a uri"))
                .isInstanceOf(ParameterException.class);
    }

    @Test
    public void convertToRoundTripsToTheOriginalUriString() {
        final ProxyConfigConverter converter = new ProxyConfigConverter();
        final ProxyConfig config = new ProxyConfig(URI.create("http://proxy.example.com:8123"));

        assertThat(converter.convertTo(config)).isEqualTo("http://proxy.example.com:8123");
    }
}

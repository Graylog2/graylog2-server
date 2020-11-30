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

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ProxyHostsPatternConverterTest {
    @Test
    public void convertFromAndTo() {
        final ProxyHostsPatternConverter converter = new ProxyHostsPatternConverter();
        final ProxyHostsPattern pattern = converter.convertFrom("127.0.0.1,node0.graylog.example.com");

        assertThat(pattern.matches("127.0.0.1")).isTrue();
        assertThat(converter.convertTo(pattern)).isEqualTo("127.0.0.1,node0.graylog.example.com");
    }
}
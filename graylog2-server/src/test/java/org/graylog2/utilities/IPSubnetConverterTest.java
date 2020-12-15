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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class IPSubnetConverterTest {
    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    private final IPSubnetConverter converter = new IPSubnetConverter();

    @Test
    public void testDefault() throws Exception {
        final String defaultList = "127.0.0.1/32,0:0:0:0:0:0:0:1/128";
        final Set<IpSubnet> results = converter.convertFrom(defaultList);
        assertThat(results)
            .hasSize(2)
            .contains(new IpSubnet("127.0.0.1/32"))
            .contains(new IpSubnet("0:0:0:0:0:0:0:1/128"));
        assertThat(converter.convertTo(results)).isEqualTo(defaultList);
    }

    @Test
    public void testNormalize() throws Exception {
        final String defaultList = "127.0.0.1/32, ::1/128";
        final String normalized = "127.0.0.1/32,0:0:0:0:0:0:0:1/128";
        final Set<IpSubnet> results = converter.convertFrom(defaultList);
        assertThat(converter.convertTo(results)).isEqualTo(normalized);
    }

    @Test
    public void testNull() throws Exception {
        assertThat(converter.convertFrom(null)).isEmpty();
    }

    @Test
    public void convertFromThrowsParameterExceptionWithInvalidSubnet() {
        expectedException.expect(ParameterException.class);
        expectedException.expectMessage("Invalid subnet: HODOR");
        converter.convertFrom("127.0.0.1/32, ::1/128, HODOR");
    }

    @Test
    public void convertToThrowsParameterExceptionWithNull() {
        expectedException.expect(ParameterException.class);
        expectedException.expectMessage("Couldn't convert IP subnets <null> to string.");
        converter.convertTo(null);
    }
}

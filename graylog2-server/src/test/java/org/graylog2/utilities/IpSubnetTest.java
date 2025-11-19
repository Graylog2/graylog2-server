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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

public class IpSubnetTest {
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"127.0.0.1/32", "127.0.0.1", "127.0.0.1", "127.0.0.1", true},
                {"127.0.0.1/32", "127.0.0.2", "127.0.0.1", "127.0.0.1", false},
                {"::1/128", "::1", "0:0:0:0:0:0:0:1", "0:0:0:0:0:0:0:1", true},
                {"::1/128", "::2", "0:0:0:0:0:0:0:1", "0:0:0:0:0:0:0:1", false},
                {"::1/128", "127.0.0.1", "0:0:0:0:0:0:0:1", "0:0:0:0:0:0:0:1", false},
                {"127.0.0.1/32", "::1", "127.0.0.1", "127.0.0.1", false},
                {"10.0.0.0/8", "10.1.2.3", "10.0.0.0", "10.255.255.255", true},
                {"2001:DB8::/128", "2001:DB8::1:2:3:4:5", "2001:db8:0:0:0:0:0:0", "2001:db8:0:0:0:0:0:0", false},
        });
    }

    @MethodSource("data")
    @ParameterizedTest
    public void getNetworkAddress(String cidr, String ipAddress, String networkAddress, String broadcastAddress, boolean isInSubnet) throws Exception {
        final IpSubnet ipSubnet = new IpSubnet(cidr);
        assertThat(ipSubnet.getNetworkAddress()).isEqualTo(networkAddress);
        assertThat(ipSubnet.getBroadcastAddress()).isEqualTo(broadcastAddress);
        assertThat(ipSubnet.contains(ipAddress)).isEqualTo(isInSubnet);
    }

}

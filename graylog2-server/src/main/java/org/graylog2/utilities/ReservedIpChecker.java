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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ReservedIpChecker {
    private static final Logger LOG = LoggerFactory.getLogger(ReservedIpChecker.class);
    private static final String[] RESERVED_IPV4_BLOCKS = {
            "0.0.0.0/8",
            "10.0.0.0/8",
            "100.64.0.0/10",
            "127.0.0.0/8",
            "169.254.0.0/16",
            "172.16.0.0/12",
            "192.0.0.0/24",
            "192.0.2.0/24",
            "192.88.99.0/24",
            "192.168.0.0/16",
            "198.18.0.0/15",
            "198.51.100.0/24",
            "203.0.113.0/24",
            "224.0.0.0/4",
            "233.252.0.0/24",
            "240.0.0.0/4",
            "255.255.255.255/32"
    };

    private static ReservedIpChecker instance;

    private final List<IpSubnet> ipBlocks;

    public ReservedIpChecker() {
        this.ipBlocks = loadReservedIpBlocks();
    }

    private List<IpSubnet> loadReservedIpBlocks() {

        return Arrays.stream(RESERVED_IPV4_BLOCKS)
                .map(stringToSubnet())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    public boolean isEmpty() {
        return ipBlocks.isEmpty();
    }

    public boolean isReservedIpAddress(String address) {
        if (StringUtils.isBlank(address)) {
            return false;
        }

        return ipBlocks.stream().anyMatch(e -> subnetContainsAddress(e, address));
    }

    private static Function<String, Optional<IpSubnet>> stringToSubnet() {
        return line -> {
            IpSubnet subnet;
            try {
                subnet = new IpSubnet(line);
            } catch (UnknownHostException ignore) {
                subnet = null;
            }

            return Optional.ofNullable(subnet);
        };
    }

    private static boolean subnetContainsAddress(IpSubnet subnet, String targetAddress) {
        try {
            return subnet.contains(targetAddress);
        } catch (UnknownHostException ignore) {
            return false;
        }
    }


    public static synchronized ReservedIpChecker getInstance() {

        if (instance == null) {
            instance = new ReservedIpChecker();
        }

        return instance;
    }
}

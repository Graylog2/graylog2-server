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

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ReservedIpChecker {
    private static final Logger LOG = LoggerFactory.getLogger(ReservedIpChecker.class);
    public static final String IPV4_BLOCKS_FILE = "reserved-ipv4-blocks.txt";

    private static ReservedIpChecker instance;

    private final List<IpSubnet> ipBlocks;

    public ReservedIpChecker() {
        this.ipBlocks = loadReservedIpBlocks();
    }

    private List<IpSubnet> loadReservedIpBlocks() {

        List<IpSubnet> list;
        try {
            URL url = getClass().getClassLoader().getResource(IPV4_BLOCKS_FILE);
            if (url == null) {
                String error = String.format(Locale.ENGLISH, "Error.  IP Block file '%s' was not found.", IPV4_BLOCKS_FILE);
                LOG.error(error);
                list = Collections.emptyList();
            } else {
                Path path = Paths.get(url.toURI());
                list = Files.readAllLines(path)
                        .stream()
                        .map(stringToSubnet())
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toList());
            }
        } catch (IOException | URISyntaxException e) {

            String error = String.format(Locale.ENGLISH, "Error loading Reserved IP Blocks. %s", e.getMessage());
            LOG.error(error, e);
            list = Collections.emptyList();
        }

        return list;
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

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

package org.graylog.integrations;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.util.SubnetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ReservedIpChecker {
    private static final Logger LOG = LoggerFactory.getLogger(ReservedIpChecker.class);
    public static final String IPV4_BLOCKS_FILE = "reserved-ipv4-blocks.txt";

    private static ReservedIpChecker instance;

    private final List<SubnetUtils.SubnetInfo> ipBlocks;

    public ReservedIpChecker() {
        this.ipBlocks = loadReservedIpBlocks();
    }

    private List<SubnetUtils.SubnetInfo> loadReservedIpBlocks() {

        List<SubnetUtils.SubnetInfo> list;
        try {
            URL url = getClass().getClassLoader().getResource(IPV4_BLOCKS_FILE);
            if (url == null) {
                String error = String.format("Error.  IP Block file '%s' was not found.", IPV4_BLOCKS_FILE);
                LOG.error(error);
                list = Collections.emptyList();
            } else {
                Path path = Paths.get(url.toURI());
                list = Files.readAllLines(path)
                        .stream()
                        .map(line -> new SubnetUtils(line).getInfo())
                        .collect(Collectors.toList());
            }
        } catch (IOException | URISyntaxException e) {

            String error = String.format("Error loading Reserved IP Blocks.%s", e.getMessage());
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

        return ipBlocks.stream().anyMatch(e -> e.isInRange(address));

    }

    public static synchronized ReservedIpChecker getInstance() {

        if (instance == null) {
            instance = new ReservedIpChecker();
        }

        return instance;
    }
}

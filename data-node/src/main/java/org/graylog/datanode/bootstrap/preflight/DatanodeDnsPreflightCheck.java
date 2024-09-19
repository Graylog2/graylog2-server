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
package org.graylog.datanode.bootstrap.preflight;

import edu.emory.mathcs.backport.java.util.Arrays;
import jakarta.inject.Inject;
import org.graylog.datanode.Configuration;
import org.graylog2.bootstrap.preflight.PreflightCheck;
import org.graylog2.bootstrap.preflight.PreflightCheckException;
import org.graylog2.shared.SuppressForbidden;

import java.net.InetAddress;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DatanodeDnsPreflightCheck implements PreflightCheck {
    private final String configuredHostname;

    @Inject
    public DatanodeDnsPreflightCheck(Configuration datanodeConfiguration) {
        configuredHostname = datanodeConfiguration.getHostname();
    }

    @Override
    public void runCheck() throws PreflightCheckException {
        final Set<String> altNames = determineAltNames();
        if (!determineAltNames().contains(configuredHostname)) {
            throw new PreflightCheckException("Reverse lookup of the localhost IP failed. DNS is not configured properly for the hostname " + configuredHostname + ". Resolved hostnames: " + altNames);
        }
    }


    private Set<String> determineAltNames() {
        return Stream.of("127.0.0.1", "::1")
                .map(this::reverseLookup)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    @SuppressForbidden("Deliberate use of InetAddress#getHostName")
    private String reverseLookup(String ipAddress) {
        try {
            final var inetAddress = InetAddress.getByName(ipAddress);
            final var reverseLookup = inetAddress.getHostName();
            return reverseLookup.equals(ipAddress) ? null : reverseLookup;
        } catch (Exception e) {
            return null;
        }
    }
}

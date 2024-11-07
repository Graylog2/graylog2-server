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

import jakarta.inject.Inject;
import org.graylog.datanode.Configuration;
import org.graylog2.bootstrap.preflight.PreflightCheck;
import org.graylog2.bootstrap.preflight.PreflightCheckException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.stream.Stream;

public class DatanodeDnsPreflightCheck implements PreflightCheck {

    private static final Logger LOG = LoggerFactory.getLogger(DatanodeDnsPreflightCheck.class);

    private final String configuredHostname;

    @Inject
    public DatanodeDnsPreflightCheck(Configuration datanodeConfiguration) {
        configuredHostname = datanodeConfiguration.getHostname();
    }

    @Override
    public void runCheck() throws PreflightCheckException {
        try {
            final InetAddress[] addresses = InetAddress.getAllByName(configuredHostname);
            final List<String> ips = Stream.of(addresses).map(InetAddress::getHostAddress).toList();
            LOG.debug("Datanode host {} is available on {} addresses", configuredHostname, ips);
        } catch (UnknownHostException e) {
            throw new PreflightCheckException("Configured hostname " + configuredHostname + " is not bound to any address! Please configure your DNS so the hostname points to this machine");
        }
    }
}

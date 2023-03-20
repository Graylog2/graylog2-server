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
package org.graylog.plugins.threatintel.whois.ip.parsers;

import org.graylog.plugins.threatintel.whois.ip.InternetRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class WhoisParser {

    protected static final Logger LOG = LoggerFactory.getLogger(WhoisParser.class);

    protected boolean isRedirect = false;
    protected InternetRegistry registryRedirect;

    protected String organization;
    protected String countryCode;

    protected String lineValue(String line) {
        if(!line.contains(":")) {
            return "";
        }

        String[] parts = line.split(":");
        return parts[1].trim();
    }

    protected InternetRegistry findRegistryFromWhoisServer(String server) {
        for (InternetRegistry registry : InternetRegistry.values()) {
            if (registry.getWhoisServer().equals(server)) {
                return registry;
            }
        }

        LOG.error("No known internet registry for WHOIS server redirect [{}].", server);
        return null;
    }

    public String buildQueryForIp(String ip) { return ip; }

    public boolean isRedirect() {
        return isRedirect;
    }

    public InternetRegistry getRegistryRedirect() {
        return registryRedirect;
    }

    public String getOrganization() {
        return this.organization;
    }

    public String getCountryCode() {
        return this.countryCode;
    }

    public abstract void readLine(String line);

}

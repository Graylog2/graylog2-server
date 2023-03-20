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
package org.graylog.plugins.threatintel.whois.ip;

public enum InternetRegistry {

    AFRINIC("whois.afrinic.net"),
    APNIC("whois.apnic.net"),
    ARIN("whois.arin.net"),
    LACNIC("whois.lacnic.net"),
    RIPENCC("whois.ripe.net");

    final String whoisServer;

    InternetRegistry(String whoisServer) {
        this.whoisServer = whoisServer;
    }

    public String getWhoisServer() {
        return whoisServer;
    }
}

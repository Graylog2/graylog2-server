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
package org.graylog.plugins.map.geoip;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.AsnResponse;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.model.CountryResponse;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;

public class MaxMindIPLocationDatabaseAdapter implements IPLocationDatabaseAdapter {
    private final DatabaseReader reader;

    public MaxMindIPLocationDatabaseAdapter(File databaseFile) throws IOException {
        this.reader = new DatabaseReader.Builder(databaseFile).build();
    }

    @Override
    public CityResponse maxMindCity(InetAddress ipAddress) throws IOException, GeoIp2Exception {
        return reader.city(ipAddress);
    }

    @Override
    public CountryResponse maxMindCountry(InetAddress ipAddress) throws IOException, GeoIp2Exception {
        return reader.country(ipAddress);
    }

    @Override
    public AsnResponse maxMindASN(InetAddress ipAddress) throws IOException, GeoIp2Exception {
        return reader.asn(ipAddress);
    }

    @Override
    public IPinfoStandardLocation ipInfoStandardLocation(InetAddress ipAddress) throws IOException, AddressNotFoundException {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public IPinfoASN ipInfoASN(InetAddress ipAddress) throws IOException, AddressNotFoundException {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }
}

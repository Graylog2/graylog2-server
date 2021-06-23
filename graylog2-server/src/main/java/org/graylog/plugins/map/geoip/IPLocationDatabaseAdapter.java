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

import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.AsnResponse;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.model.CountryResponse;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;

/**
 * Small abstraction layer for the different location databases from MaxMind and IPinfo to make them usable in a
 * single lookup data adapter until we create separate adapters for the different databases.
 */
public interface IPLocationDatabaseAdapter extends Closeable {
    CityResponse maxMindCity(InetAddress ipAddress) throws IOException, GeoIp2Exception;

    CountryResponse maxMindCountry(InetAddress ipAddress) throws IOException, GeoIp2Exception;

    AsnResponse maxMindASN(InetAddress ipAddress) throws IOException, GeoIp2Exception;

    IPinfoStandardLocation ipInfoStandardLocation(InetAddress ipAddress) throws IOException, AddressNotFoundException;

    IPinfoASN ipInfoASN(InetAddress ipAddress) throws IOException, AddressNotFoundException;
}

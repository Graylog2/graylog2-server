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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.maxmind.db.InvalidDatabaseException;
import com.maxmind.db.NoCache;
import com.maxmind.db.Reader;
import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.AsnResponse;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.model.CountryResponse;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;

/**
 * Custom {@link Reader} wrapper to be able to support IPinfo database files. The IPinfo databases use a different
 * database type identifier and a different data format than the MaxMind databases.
 */
public class IPinfoIPLocationDatabaseAdapter implements IPLocationDatabaseAdapter {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(MapperFeature.CAN_OVERRIDE_ACCESS_MODIFIERS, false)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);

    private final Reader reader;

    public IPinfoIPLocationDatabaseAdapter(File databaseFile) throws IOException {
        this.reader = new Reader(databaseFile, Reader.FileMode.MEMORY_MAPPED, NoCache.getInstance());
    }

    private ObjectNode get(InetAddress ipAddress, String type) throws IOException, AddressNotFoundException {
        final String databaseType = reader.getMetadata().getDatabaseType();
        if (!databaseType.contains(type)) {
            final String caller = Thread.currentThread().getStackTrace()[2].getMethodName();
            throw new UnsupportedOperationException("Invalid attempt to open a \"" + databaseType + "\" database using the " + caller + " method");
        }

        final ObjectNode node = asObjectNode(reader.get(ipAddress));
        if (node == null) {
            throw new AddressNotFoundException("Address " + ipAddress.getHostAddress() + " not found in database");
        }

        return node;
    }

    private ObjectNode asObjectNode(JsonNode node) throws InvalidDatabaseException {
        if (node == null || node instanceof ObjectNode) {
            return (ObjectNode) node;
        }
        throw new InvalidDatabaseException("Unexpected data type returned. The IPinfo database may be corrupt.");
    }

    @Override
    public IPinfoStandardLocation ipInfoStandardLocation(InetAddress ipAddress) throws IOException, AddressNotFoundException {
        return OBJECT_MAPPER.convertValue(get(ipAddress, "ipinfo standard_location"), IPinfoStandardLocation.class);
    }

    @Override
    public IPinfoASN ipInfoASN(InetAddress ipAddress) throws IOException, AddressNotFoundException {
        return OBJECT_MAPPER.convertValue(get(ipAddress, "ipinfo asn"), IPinfoASN.class);
    }

    @Override
    public AsnResponse maxMindASN(InetAddress ipAddress) throws IOException, GeoIp2Exception {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public CountryResponse maxMindCountry(InetAddress ipAddress) throws IOException, GeoIp2Exception {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public CityResponse maxMindCity(InetAddress ipAddress) throws IOException, GeoIp2Exception {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public void close() throws IOException {
        this.reader.close();
    }
}

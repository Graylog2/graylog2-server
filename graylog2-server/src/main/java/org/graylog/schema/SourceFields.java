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
package org.graylog.schema;

public class SourceFields {
    private static final String SOURCE_PREFIX = "source_";

    public static final String SOURCE_BYTES_SENT = "source_bytes_sent";
    public static final String SOURCE_DEVICE_MODEL = "source_device_model";
    public static final String SOURCE_DEVICE_VENDOR = "source_device_vendor";
    public static final String SOURCE_HOSTNAME = "source_hostname";
    public static final String SOURCE_ID = "source_id";
    public static final String SOURCE_IP = "source_ip";
    public static final String SOURCE_IPV6 = "source_ipv6";
    public static final String SOURCE_NAT_IP = "source_nat_ip";
    public static final String SOURCE_NAT_PORT = "source_nat_port";
    public static final String SOURCE_OS_NAME = "source_os_name";
    public static final String SOURCE_OS_VERSION = "source_os_version";
    public static final String SOURCE_PACKETS_SENT = "source_packets_sent";
    public static final String SOURCE_PORT = "source_port";
    public static final String SOURCE_TYPE = "source_type";
    public static final String SOURCE_VSYS_UUID = "source_vsys_uuid";
    public static final String SOURCE_ZONE = "source_zone";

    // Derived and Enriched Fields
    public static final String SOURCE_CATEGORY = "source_category";
    public static final String SOURCE_LOCATION_NAME = "source_location_name";
    public static final String SOURCE_MAC = "source_mac";
    public static final String SOURCE_PRIORITY = "source_priority";
    public static final String SOURCE_PRIORITY_LEVEL = "source_priority_level";
    public static final String SOURCE_REFERENCE = "source_reference";

    // Autonomous System Fields
    public static final String SOURCE_AS_DOMAIN = SOURCE_PREFIX + AutonomousSystemFields.AS_DOMAIN;
    public static final String SOURCE_AS_ISP = SOURCE_PREFIX + AutonomousSystemFields.AS_ISP;
    public static final String SOURCE_AS_NUMBER = SOURCE_PREFIX + AutonomousSystemFields.AS_NUMBER;
    public static final String SOURCE_AS_ORGANIZATION = SOURCE_PREFIX + AutonomousSystemFields.AS_ORGANIZATION;

    // Geo Fields
    public static final String SOURCE_GEO_CITY_NAME = SOURCE_PREFIX + GeoFields.GEO_CITY_NAME;
    public static final String SOURCE_GEO_COUNTRY_ISO = SOURCE_PREFIX + GeoFields.GEO_COUNTRY_ISO;
    public static final String SOURCE_GEO_COUNTRY_NAME = SOURCE_PREFIX + GeoFields.GEO_COUNTRY_NAME;
    public static final String SOURCE_GEO_COORDINATES = SOURCE_PREFIX + GeoFields.GEO_COORDINATES;
    public static final String SOURCE_GEO_NAME = SOURCE_PREFIX + GeoFields.GEO_NAME;
    public static final String SOURCE_GEO_STATE_NAME = SOURCE_PREFIX + GeoFields.GEO_STATE_NAME;

    // User Fields
    public static final String SOURCE_USER = SOURCE_PREFIX + UserFields.USER;
    public static final String SOURCE_USER_EMAIL = SOURCE_PREFIX + UserFields.USER_EMAIL;
    public static final String SOURCE_USER_NAME = SOURCE_PREFIX + UserFields.USER_NAME;

    // To be removed
    @Deprecated
    public static final String SOURCE_PACKETS = SOURCE_PACKETS_SENT;
    @Deprecated
    public static final String SOURCE_AS_ORGANIZATION_NAME = SOURCE_PREFIX + AutonomousSystemFields.AS_ORGANIZATION;
}

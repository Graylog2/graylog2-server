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

public class DestinationFields {
    private static final String DESTINATION_PREFIX = "destination_";

    public static final String DESTINATION_APPLICATION_NAME = "destination_application_name";
    public static final String DESTINATION_BYTES_SENT = "destination_bytes_sent";
    public static final String DESTINATION_DEVICE_MODEL = "destination_device_model";
    public static final String DESTINATION_DEVICE_VENDOR = "destination_device_vendor";
    public static final String DESTINATION_DOMAIN = "destination_domain";
    public static final String DESTINATION_HOSTNAME = "destination_hostname";
    public static final String DESTINATION_ID = "destination_id";
    public static final String DESTINATION_IP = "destination_ip";
    public static final String DESTINATION_NAT_IP = "destination_nat_ip";
    public static final String DESTINATION_NAT_PORT = "destination_nat_port";
    public static final String DESTINATION_OS_NAME = "destination_os_name";
    public static final String DESTINATION_OS_VERSION = "destination_os_version";
    public static final String DESTINATION_PACKETS_SENT = "destination_packets_sent";
    public static final String DESTINATION_PORT = "destination_port";
    public static final String DESTINATION_TYPE = "destination_type";
    public static final String DESTINATION_VSYS_UUID = "destination_vsys_uuid";
    public static final String DESTINATION_ZONE = "destination_zone";

    // Derived and Enriched Fields
    public static final String DESTINATION_CATEGORY = "destination_category";
    public static final String DESTINATION_LOCATION_NAME = "destination_location_name";
    public static final String DESTINATION_MAC = "destination_mac";
    public static final String DESTINATION_PRIORITY = "destination_priority";
    public static final String DESTINATION_PRIORITY_LEVEL = "destination_priority_level";
    public static final String DESTINATION_REFERENCE = "destination_reference";

    // Autonomous System Fields
    public static final String DESTINATION_AS_DOMAIN = DESTINATION_PREFIX + AutonomousSystemFields.AS_DOMAIN;
    public static final String DESTINATION_AS_ISP = DESTINATION_PREFIX + AutonomousSystemFields.AS_ISP;
    public static final String DESTINATION_AS_NUMBER = DESTINATION_PREFIX + AutonomousSystemFields.AS_NUMBER;
    public static final String DESTINATION_AS_ORGANIZATION = DESTINATION_PREFIX + AutonomousSystemFields.AS_ORGANIZATION;

    // Geo Fields
    public static final String DESTINATION_GEO_CITY_NAME = DESTINATION_PREFIX + GeoFields.GEO_CITY_NAME;
    public static final String DESTINATION_GEO_COUNTRY_ISO = DESTINATION_PREFIX + GeoFields.GEO_COUNTRY_ISO;
    public static final String DESTINATION_GEO_COUNTRY_NAME = DESTINATION_PREFIX + GeoFields.GEO_COUNTRY_NAME;
    public static final String DESTINATION_GEO_COORDINATES = DESTINATION_PREFIX + GeoFields.GEO_COORDINATES;
    public static final String DESTINATION_GEO_NAME = DESTINATION_PREFIX + GeoFields.GEO_NAME;
    public static final String DESTINATION_GEO_STATE_NAME = DESTINATION_PREFIX + GeoFields.GEO_STATE_NAME;

    // User Fields
    public static final String DESTINATION_USER_NAME = DESTINATION_PREFIX + UserFields.USER_NAME;

    // To be removed
    @Deprecated
    public static final String DESTINATION_PACKETS = DESTINATION_PACKETS_SENT;
}

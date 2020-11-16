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

public class HostFields {
    private static final String HOST_PREFIX = "host_";

    public static final String HOST_HOSTNAME = "host_hostname";
    public static final String HOST_ID = "host_id";
    public static final String HOST_IP = "host_ip";
    public static final String HOST_REFERENCE = "host_reference";
    public static final String HOST_VIRTFW_HOSTNAME = "host_virtfw_hostname";
    public static final String HOST_VIRTFW_ID = "host_virtfw_id";
    public static final String HOST_VIRTFW_UID = "host_virtfw_uid";

    // Derived and Enriched Fields
    public static final String HOST_CATEGORY = "host_category";
    public static final String HOST_LOCATION_NAME = "host_location_name";
    public static final String HOST_MAC = "host_mac";
    public static final String HOST_PRIORITY = "host_priority";
    public static final String HOST_PRIORITY_LEVEL = "host_priority_level";
    public static final String HOST_TYPE = "host_type";
    public static final String HOST_TYPE_VERSION = "host_type_version";

    // Autonomous System Fields
    public static final String HOST_AS_DOMAIN = HOST_PREFIX + AutonomousSystemFields.AS_DOMAIN;
    public static final String HOST_AS_ISP = HOST_PREFIX + AutonomousSystemFields.AS_ISP;
    public static final String HOST_AS_NUMBER = HOST_PREFIX + AutonomousSystemFields.AS_NUMBER;
    public static final String HOST_AS_ORGANIZATION_NAME = HOST_PREFIX + AutonomousSystemFields.AS_ORGANIZATION_NAME;

    // Geo Fields
    public static final String HOST_GEO_CITY_ISO_CODE = HOST_PREFIX + GeoFields.GEO_CITY_ISO_CODE;
    public static final String HOST_GEO_CITY_NAME = HOST_PREFIX + GeoFields.GEO_CITY_NAME;
    public static final String HOST_GEO_STATE_NAME = HOST_PREFIX + GeoFields.GEO_STATE_NAME;
    public static final String HOST_GEO_COUNTRY_ISO_CODE = HOST_PREFIX + GeoFields.GEO_COUNTRY_ISO_CODE;
    public static final String HOST_GEO_COUNTRY_NAME = HOST_PREFIX + GeoFields.GEO_COUNTRY_NAME;
    public static final String HOST_GEO_COORDINATES = HOST_PREFIX + GeoFields.GEO_COORDINATES;
}

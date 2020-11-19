/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.schema;

public class HostFields {
    private static final String HOST_PREFIX = "host_";

    public static final String HOST_HOSTNAME = "host_hostname";
    public static final String HOST_ID = "host_id";
    public static final String HOST_IP = "host_ip";
    public static final String HOST_IPV6 = "host_ipv6";
    public static final String HOST_MAC = "host_mac";
    public static final String HOST_REFERENCE = "host_reference";
    public static final String HOST_TYPE_VERSION = "host_type_version";
    public static final String HOST_VIRTFW_HOSTNAME = "host_virtfw_hostname";
    public static final String HOST_VIRTFW_ID = "host_virtfw_id";
    public static final String HOST_VIRTFW_UID = "host_virtfw_uid";

    // Derived and Enriched Fields
    public static final String HOST_CATEGORY = "host_category";
    public static final String HOST_LOCATION_NAME = "host_location_name";
    public static final String HOST_PRIORITY = "host_priority";
    public static final String HOST_PRIORITY_LEVEL = "host_priority_level";
    public static final String HOST_TYPE = "host_type";

    // Autonomous System Fields
    public static final String HOST_AS_DOMAIN = HOST_PREFIX + AutonomousSystemFields.AS_DOMAIN;
    public static final String HOST_AS_ISP = HOST_PREFIX + AutonomousSystemFields.AS_ISP;
    public static final String HOST_AS_NUMBER = HOST_PREFIX + AutonomousSystemFields.AS_NUMBER;
    public static final String HOST_AS_ORGANIZATION = HOST_PREFIX + AutonomousSystemFields.AS_ORGANIZATION;

    // Geo Fields
    public static final String HOST_GEO_CITY_NAME = HOST_PREFIX + GeoFields.GEO_CITY_NAME;
    public static final String HOST_GEO_COUNTRY_ISO = HOST_PREFIX + GeoFields.GEO_COUNTRY_ISO;
    public static final String HOST_GEO_COUNTRY_NAME = HOST_PREFIX + GeoFields.GEO_COUNTRY_NAME;
    public static final String HOST_GEO_COORDINATES = HOST_PREFIX + GeoFields.GEO_COORDINATES;
    public static final String HOST_GEO_NAME = HOST_PREFIX + GeoFields.GEO_NAME;
    public static final String HOST_GEO_STATE_NAME = HOST_PREFIX + GeoFields.GEO_STATE_NAME;
}

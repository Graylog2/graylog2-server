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

public class SourceFields {
    private static final String SOURCE_PREFIX = "source_";

    public static final String SOURCE_BYTES_SENT = "source_bytes_sent";
    public static final String SOURCE_HOSTNAME = "source_hostname";
    public static final String SOURCE_IP = "source_ip";
    public static final String SOURCE_IPV6 = "source_ipv6";
    public static final String SOURCE_NAT_IP = "source_nat_ip";
    public static final String SOURCE_NAT_PORT = "source_nat_port";
    public static final String SOURCE_PACKETS_SENT = "source_packets_sent";
    public static final String SOURCE_PORT = "source_port";
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
    public static final String SOURCE_AS_ORGANIZATION_NAME = SOURCE_PREFIX + AutonomousSystemFields.AS_ORGANIZATION_NAME;

    // Geo Fields
    public static final String SOURCE_GEO_CITY_ISO_CODE = SOURCE_PREFIX + GeoFields.GEO_CITY_ISO_CODE;
    public static final String SOURCE_GEO_CITY_NAME = SOURCE_PREFIX + GeoFields.GEO_CITY_NAME;
    public static final String SOURCE_GEO_STATE_NAME = SOURCE_PREFIX + GeoFields.GEO_STATE_NAME;
    public static final String SOURCE_GEO_COUNTRY_ISO_CODE = SOURCE_PREFIX + GeoFields.GEO_COUNTRY_ISO_CODE;
    public static final String SOURCE_GEO_COUNTRY_NAME = SOURCE_PREFIX + GeoFields.GEO_COUNTRY_NAME;
    public static final String SOURCE_GEO_COORDINATES = SOURCE_PREFIX + GeoFields.GEO_COORDINATES;

    // To be removed
    @Deprecated
    public static final String SOURCE_PACKETS = "source_packets_sent";
    @Deprecated
    public static final String SOURCE_USER = "source_user";
    @Deprecated
    public static final String SOURCE_USER_EMAIL = "source_user_email";
    @Deprecated
    public static final String SOURCE_GEO_ISO_CODE = SOURCE_PREFIX + GeoFields.GEO_ISO_CODE;

}

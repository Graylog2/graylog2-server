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

public class DestinationFields {
    private static final String DESTINATION_PREFIX = "destination_";

    public static final String DESTINATION_APPLICATION_NAME = "destination_application_name";
    public static final String DESTINATION_BYTES_SENT = "destination_bytes_sent";
    public static final String DESTINATION_HOSTNAME = "destination_hostname";
    public static final String DESTINATION_IP = "destination_ip";
    public static final String DESTINATION_NAT_IP = "destination_nat_ip";
    public static final String DESTINATION_NAT_PORT = "destination_nat_port";
    public static final String DESTINATION_PACKETS_SENT = "destination_packets_sent";
    public static final String DESTINATION_PORT = "destination_port";
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
    public static final String DESTINATION_AS_ORGANIZATION_NAME = DESTINATION_PREFIX + AutonomousSystemFields.AS_ORGANIZATION_NAME;

    // Geo Fields
    public static final String DESTINATION_GEO_CITY_ISO_CODE = DESTINATION_PREFIX + GeoFields.GEO_CITY_ISO_CODE;
    public static final String DESTINATION_GEO_CITY_NAME = DESTINATION_PREFIX + GeoFields.GEO_CITY_NAME;
    public static final String DESTINATION_GEO_STATE_NAME = DESTINATION_PREFIX + GeoFields.GEO_STATE_NAME;
    public static final String DESTINATION_GEO_COUNTRY_ISO_CODE = DESTINATION_PREFIX + GeoFields.GEO_COUNTRY_ISO_CODE;
    public static final String DESTINATION_GEO_COUNTRY_NAME = DESTINATION_PREFIX + GeoFields.GEO_COUNTRY_NAME;
    public static final String DESTINATION_GEO_COORDINATES = DESTINATION_PREFIX + GeoFields.GEO_COORDINATES;

    // To be removed
    @Deprecated
    public static final String DESTINATION_PACKETS = "destination_packets_sent";
    @Deprecated
    public static final String DESTINATION_GEO_ISO_CODE = DESTINATION_PREFIX + GeoFields.GEO_ISO_CODE;
}

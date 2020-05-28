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

public enum DestinationFields {
    DESTINATION_APPLICATION_NAME("destination_application_name"),
    DESTINATION_BYTES("destination_bytes"),
    DESTINATION_HOSTNAME("destination_hostname"),
    DESTINATION_IP("destination_ip"),
    DESTINATION_NAT_IP("destination_nat_ip"),
    DESTINATION_NAT_PORT("destination_nat_port"),
    DESTINATION_PACKETS("destination_packets"),
    DESTINATION_PORT("destination_port"),
    DESTINATION_VSYS_UUID("destination_vsys_uuid"),
    DESTINATION_ZONE("destination_zone"),

    // Derived and Enriched Fields
    DESTINATION_CATEGORY("destination_category"),
    DESTINATION_LOCATION_NAME("destination_location_name"),
    DESTINATION_MAC("destination_mac"),
    DESTINATION_PRIORITY("destination_priority"),
    DESTINATION_PRIORITY_LEVEL("destination_priority_level"),
    DESTINATION_REFERENCE("destination_reference"),

    // Autonomous System Fields
    DESTINATION_AS_DOMAIN("destination_as_domain"),
    DESTINATION_AS_ISP("destination_as_isp"),
    DESTINATION_AS_NUMBER("destination_as_number"),
    DESTINATION_AS_ORGANIZATION_NAME("destination_as_organization_name"),

    // Geo Fields
    DESTINATION_GEO_CITY_NAME("destination_geo_city_name"),
    DESTINATION_GEO_STATE_NAME("destination_geo_state_name"),
    DESTINATION_GEO_ISO_CODE("destination_geo_iso_code"),
    DESTINATION_GEO_COUNTRY_NAME("destination_geo_country_name"),
    DESTINATION_GEO_COORDINATES("destination_geo_coordinates");

    private String value;

    DestinationFields(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}

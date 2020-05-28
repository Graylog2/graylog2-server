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

public enum SourceFields {
    SOURCE_BYTES("source_bytes"),
    SOURCE_HOSTNAME("source_hostname"),
    SOURCE_IP("source_ip"),
    SOURCE_NAT_IP("source_nat_ip"),
    SOURCE_NAT_PORT("source_nat_port"),
    SOURCE_PACKETS("source_packets"),
    SOURCE_PORT("source_port"),
    SOURCE_USER("source_user"),
    SOURCE_USER_EMAIL("source_user_email"),
    SOURCE_VSYS_UUID("source_vsys_uuid"),
    SOURCE_ZONE("source_zone"),

    // Derived and Enriched Fields
    SOURCE_CATEGORY("source_category"),
    SOURCE_LOCATION_NAME("source_location_name"),
    SOURCE_MAC("source_mac"),
    SOURCE_PRIORITY("source_priority"),
    SOURCE_PRIORITY_LEVEL("source_priority_level"),
    SOURCE_REFERENCE("source_reference"),

    // Autonomous System Fields
    SOURCE_AS_DOMAIN("source_as_domain"),
    SOURCE_AS_ISP("source_as_isp"),
    SOURCE_AS_NUMBER("source_as_number"),
    SOURCE_AS_ORGANIZATION_NAME("source_as_organization_name"),

    // Geo Fields
    SOURCE_GEO_CITY_NAME("source_geo_city_name"),
    SOURCE_GEO_STATE_NAME("source_geo_state_name"),
    SOURCE_GEO_ISO_CODE("source_geo_iso_code"),
    SOURCE_GEO_COUNTRY_NAME("source_geo_country_name"),
    SOURCE_GEO_COORDINATES("source_geo_coordinates");

    private String value;

    SourceFields(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}

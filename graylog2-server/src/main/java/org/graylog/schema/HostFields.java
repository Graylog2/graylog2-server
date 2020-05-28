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

public enum HostFields {
    HOST_HOSTNAME("host_hostname"),
    HOST_ID("host_id"),
    HOST_IP("host_ip"),
    HOST_REFERENCE("host_reference"),
    HOST_VIRTFW_HOSTNAME("host_virtfw_hostname"),
    HOST_VIRTFW_ID("host_virtfw_id"),
    HOST_VIRTFW_UID("host_virtfw_uid"),

    // Derived and Enriched Fields
    HOST_CATEGORY("host_category"),
    HOST_LOCATION_NAME("host_location_name"),
    HOST_MAC("host_mac"),
    HOST_PRIORITY("host_priority"),
    HOST_PRIORITY_LEVEL("host_priority_level"),
    HOST_TYPE("host_type"),
    HOST_TYPE_VERSION("host_type_version"),

    // Autonomous System Fields
    HOST_AS_DOMAIN("host_as_domain"),
    HOST_AS_ISP("host_as_isp"),
    HOST_AS_NUMBER("host_as_number"),
    HOST_AS_ORGANIZATION_NAME("host_as_organization_name"),

    // Geo Fields
    HOST_GEO_CITY_NAME("host_geo_city_name"),
    HOST_GEO_STATE_NAME("host_geo_state_name"),
    HOST_GEO_ISO_CODE("host_geo_iso_code"),
    HOST_GEO_COUNTRY_NAME("host_geo_country_name"),
    HOST_GEO_COORDINATES("host_geo_coordinates");

    private String value;

    HostFields(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}

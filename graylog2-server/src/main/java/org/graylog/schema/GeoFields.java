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

public class GeoFields {
    public static final String GEO_CITY_ISO_CODE = "geo_city_iso_code";
    public static final String GEO_CITY_NAME = "geo_city_name";
    public static final String GEO_STATE_NAME = "geo_state_name";
    public static final String GEO_COUNTRY_ISO_CODE = "geo_country_iso_code";
    public static final String GEO_COUNTRY_NAME = "geo_country_name";
    public static final String GEO_COORDINATES = "geo_coordinates";

    // To be removed
    @Deprecated
    public static final String GEO_ISO_CODE = "geo_iso_code";
}

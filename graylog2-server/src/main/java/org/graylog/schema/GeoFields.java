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

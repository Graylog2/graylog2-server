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

public class AlertFields {
    public static final String ALERT_CATEGORY = "alert_category";
    public static final String ALERT_DEFINITIONS_VERSION = "alert_definitions_version";
    public static final String ALERT_INDICATOR = "alert_indicator";
    public static final String ALERT_SIGNATURE = "alert_signature";
    public static final String ALERT_SIGNATURE_ID = "alert_signature_id";

    // Derived and Enriched Fields
    public static final String ALERT_SEVERITY = "alert_severity";
    public static final String ALERT_SEVERITY_LEVEL = "alert_severity_level";
}

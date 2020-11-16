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

public class EventFields {
    public static final String EVENT_CODE = "event_code";
    public static final String EVENT_CREATED = "event_created";
    public static final String EVENT_DURATION = "event_duration";
    public static final String EVENT_ERROR_CODE = "event_error_code";
    public static final String EVENT_ERROR_DESCRIPTION = "event_error_description";
    public static final String EVENT_LOG_NAME = "event_log_name";
    public static final String EVENT_OBSERVER_HOSTNAME = "event_observer_hostname";
    public static final String EVENT_OBSERVER_ID = "event_observer_id";
    public static final String EVENT_OBSERVER_IP = "event_observer_ip";
    public static final String EVENT_OBSERVER_UID = "event_observer_uid";
    public static final String EVENT_RECEIVED_TIME = "event_received_time";
    public static final String EVENT_REPEAT_COUNT = "event_repeat_count";
    public static final String EVENT_REPORTER = "event_reporter";
    public static final String EVENT_SOURCE = "event_source";
    public static final String EVENT_SOURCE_API_VERSION = "event_source_api_version";
    public static final String EVENT_SOURCE_PRODUCT = "event_source_product";
    public static final String EVENT_START = "event_start";
    public static final String EVENT_UID = "event_uid";

    // Derived and Enriched Fields
    public static final String EVENT_ACTION = "event_action";
    public static final String EVENT_OUTCOME = "event_outcome";
    public static final String EVENT_SEVERITY = "event_severity";
    public static final String EVENT_SEVERITY_LEVEL = "event_severity_level";
}

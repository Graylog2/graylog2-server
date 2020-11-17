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

/**
 * Field names used in the standard Graylog Schema.
 *
 * @deprecated Please use the appropriate enums in this package rather than this collection of strings
 */
@Deprecated
public class GraylogSchemaFields {

    public static final String FIELD_USER_ID = "user_id";
    public static final String FIELD_ASSOCIATED_USER_REFERENCE = "associated_user_reference";
    public static final String FIELD_USER_NAME = "user_name";
    public static final String FIELD_HTTP_USER_AGENT = "http_user_agent";
    public static final String FIELD_HTTP_USER_AGENT_OS = "http_user_agent_os";
    public static final String FIELD_HTTP_USER_AGENT_NAME = "http_user_agent_name";
    public static final String FIELD_SOURCE_IP = "source_ip";
    public static final String FIELD_SOURCE_GEO_CITY_NAME = "source_geo_city_name";
    public static final String FIELD_SOURCE_GEO_STATE_NAME = "source_geo_state_name";
    public static final String FIELD_SOURCE_GEO_COUNTRY_NAME = "source_geo_country_name";
    public static final String FIELD_SOURCE_GEO_COORDINATES = "source_geo_coordinates";
    public static final String FIELD_SESSION_ID = "session_id";
    public static final String FIELD_EVENT_ERROR_DESCRIPTION = "event_error_description";
    public static final String FIELD_TIMESTAMP = "timestamp";
    public static final String FIELD_SOURCE_AS_NUMBER = "source_as_number";
    public static final String FIELD_SOURCE_AS_ORGANIZATION_NAME = "source_as_organization_name";
    public static final String FIELD_SOURCE_AS_IP = "source_as_ip";
    public static final String FIELD_SOURCE_AS_DOMAIN = "source_as_domain";
    public static final String FIELD_SERVICE_VERSION = "service_version";
    public static final String FIELD_TARGET_USER_NAME = "target_user_name";
    public static final String FIELD_TARGET_USER_ID = "target_user_id";
    public static final String FIELD_ASSOCIATED_USER_NAME = "associated_user_name";
    public static final String FIELD_ASSOCIATED_USER_ID = "associated_user_id";
    public static final String FIELD_EVENT_UID = "event_uid";
    public static final String FIELD_EVENT_SOURCE_PRODUCT = "event_source_product";

    public static final String FIELD_APPLICATION_SSO_SIGNONMODE = "application_sso_signonmode";
    public static final String FIELD_APPLICATION_SSO_TARGET_NAME = "application_sso_target_name";

    public static final String FIELD_VENDOR_EVENT_ACTION = "vendor_event_action";
    public static final String FIELD_VENDOR_EVENT_DESCRIPTION = "vendor_event_description";
    public static final String FIELD_VENDOR_EVENT_SEVERITY = "vendor_event_severity";
    public static final String FIELD_VENDOR_EVENT_OUTCOME = "vendor_event_outcome";
    public static final String FIELD_VENDOR_EVENT_OUTCOME_REASON = "vendor_event_outcome_reason";
    public static final String FIELD_VENDOR_SEVERITY_DESCRIPTION = "vendor_severity_description";
    public static final String FIELD_VENDOR_THREAT_SUSPECTED = "vendor_threat_suspected";
    public static final String FIELD_VENDOR_TRANSACTION_TYPE = "vendor_transaction_type";
    public static final String FIELD_VENDOR_TRANSACTION_ID = "vendor_transaction_id";
    public static final String FIELD_VENDOR_USER_TYPE = "vendor_user_type";

    public static final String FIELD_ILLUMINATE_EVENT_CATEGORY = "gl2_event_category";
    public static final String FIELD_ILLUMINATE_EVENT_SUBCATEGORY = "gl2_event_subcategory";
    public static final String FIELD_ILLUMINATE_EVENT_TYPE = "gl2_event_type";
    public static final String FIELD_ILLUMINATE_EVENT_TYPE_CODE = "gl2_event_type_code";
    public static final String FIELD_ILLUMINATE_TAGS = "gl2_tags";
}

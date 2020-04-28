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

/**
 * Field names used in the standard Graylog Schema.
 */
public class GraylogSchemaFields {
    public static final String FIELD_USER_ID = "user_id";
    public static final String FIELD_USER_TYPE = "user_type";
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
    public static final String FIELD_EVENT_VENDOR_DESCRIPTION = "event_vendor_description";
    public static final String FIELD_EVENT_VENDOR_ACTION = "event_vendor_action";
    public static final String FIELD_EVENT_ERROR_DESCRIPTION = "event_error_description";
    public static final String FIELD_TIMESTAMP = "timestamp";
    public static final String FIELD_SOURCE_AS_NUMBER = "source_as_number";
    public static final String FIELD_SOURCE_AS_ORGANIZATION_NAME = "source_as_organization_name";
    public static final String FIELD_SOURCE_AS_IP = "source_as_ip";
    public static final String FIELD_SOURCE_AS_DOMAIN = "source_as_domain";
    public static final String FIELD_EVENT_VENDOR_SEVERITY_DESCRIPTION = "event_vendor_severity_description";
    public static final String FIELD_THREAT_DETECTED = "threat_detected";
    public static final String FIELD_EVENT_UID = "event_uid";
    public static final String FIELD_SERVICE_VERSION = "service_version";
    public static final String FIELD_TARGET_USER_NAME = "target_user_name";
    public static final String FIELD_TARGET_USER_ID = "target_user_id";
    public static final String FIELD_ASSOCIATED_USER_NAME = "associated_user_name";
    public static final String FIELD_ASSOCIATED_USER_ID = "associated_user_id";
}

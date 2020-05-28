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

public enum EventFields {
    EVENT_CODE("event_code"),
    EVENT_CREATED("event_created"),
    EVENT_DURATION("event_duration"),
    EVENT_ERROR_CODE("event_error_code"),
    EVENT_ERROR_DESCRIPTION("event_error_description"),
    EVENT_LOG_NAME("event_log_name"),
    EVENT_RECEIVED_TIME("event_received_time"),
    EVENT_REPEAT_COUNT("event_repeat_count"),
    EVENT_REPORTER("event_reporter"),
    EVENT_SOURCE("event_source"),
    EVENT_SOURCE_API_VERSION("event_source_api_version"),
    EVENT_SOURCE_PRODUCT("event_source_product"),
    EVENT_START("event_start"),
    EVENT_UID("event_uid"),

    // Derived and Enriched Fields
    EVENT_ACTION("event_action"),
    EVENT_OUTCOME("event_outcome"),
    EVENT_SEVERITY("event_severity"),
    EVENT_SEVERITY_LEVEL("event_severity_level");

    private String value;

    EventFields(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}

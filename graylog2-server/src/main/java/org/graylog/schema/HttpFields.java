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

public enum HttpFields {
    HTTP_APPLICATION("http_application"),
    HTTP_BYTES("http_bytes"),
    HTTP_CONTENT_TYPE("http_content_type"),
    HTTP_HEADERS("http_headers"),
    HTTP_HOST("http_host"),
    HTTP_METHOD("http_method"),
    HTTP_REFERER("http_referrer"),
    HTTP_REQUEST_BYTES("http_request_bytes"),
    HTTP_REQUEST_METHOD("http_request_method"),
    HTTP_RESPONSE("http_response"),
    HTTP_RESPONSE_BYTES("http_response_bytes"),
    HTTP_RESPONSE_CODE("http_response_code"),
    HTTP_URL("http_url"),
    HTTP_URL_CATEGORY("http_url_category"),
    HTTP_USER_AGENT("http_user_agent"),
    HTTP_USER_AGENT_NAME("http_user_agent_name"),
    HTTP_USER_AGENT_OS("http_user_agent_os"),
    HTTP_VERSION("http_version"),
    HTTP_XFF("http_version"),

    // Derived and Enriched Fields
    HTTP_URL_ANALYZED("http_url_analyzed"),
    HTTP_URL_LENGTH("http_url_length"),
    HTTP_USER_AGENT_ANALYZED("http_user_agent_analyzed"),
    HTTP_USER_AGENT_LENGTH("http_user_agent_length");

    private String value;

    HttpFields(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}

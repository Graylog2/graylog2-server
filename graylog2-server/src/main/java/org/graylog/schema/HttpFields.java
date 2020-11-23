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

public class HttpFields {
    public static final String HTTP_APPLICATION = "http_application";
    public static final String HTTP_BYTES = "http_bytes";
    public static final String HTTP_CONTENT_TYPE = "http_content_type";
    public static final String HTTP_HEADERS = "http_headers";
    public static final String HTTP_HOST = "http_host";
    public static final String HTTP_METHOD = "http_method";
    public static final String HTTP_REFERER = "http_referrer";
    public static final String HTTP_REQUEST_BYTES = "http_request_bytes";
    public static final String HTTP_REQUEST_PATH = "http_request_path";
    public static final String HTTP_RESPONSE = "http_response";
    public static final String HTTP_RESPONSE_BYTES = "http_response_bytes";
    public static final String HTTP_RESPONSE_CODE = "http_response_code";
    public static final String HTTP_URI = "http_uri";
    public static final String HTTP_URI_CATEGORY = "http_uri_category";
    public static final String HTTP_USER_AGENT = "http_user_agent";
    public static final String HTTP_USER_AGENT_NAME = "http_user_agent_name";
    public static final String HTTP_USER_AGENT_OS = "http_user_agent_os";
    public static final String HTTP_VERSION = "http_version";
    public static final String HTTP_XFF = "http_xff";

    // Derived and Enriched Fields
    public static final String HTTP_REQUEST_PATH_ANALYZED = "http_request_path_analyzed";
    public static final String HTTP_URI_ANALYZED = "http_uri_analyzed";
    public static final String HTTP_URI_LENGTH = "http_uri_length";
    public static final String HTTP_USER_AGENT_ANALYZED = "http_user_agent_analyzed";
    public static final String HTTP_USER_AGENT_LENGTH = "http_user_agent_length";

    // To be removed
    @Deprecated
    public static final String HTTP_URL = HTTP_URI;
    @Deprecated
    public static final String HTTP_URL_CATEGORY = HTTP_URI_CATEGORY;
}

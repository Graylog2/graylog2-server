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

public class HttpFields {
    public static final String HTTP_APPLICATION = "http_application";
    public static final String HTTP_BYTES = "http_bytes";
    public static final String HTTP_CONTENT_TYPE = "http_content_type";
    public static final String HTTP_HEADERS = "http_headers";
    public static final String HTTP_HOST = "http_host";
    public static final String HTTP_METHOD = "http_method";
    public static final String HTTP_REFERER = "http_referrer";
    public static final String HTTP_REQUEST_BYTES = "http_request_bytes";
    public static final String HTTP_REQUEST_METHOD = "http_request_method";
    public static final String HTTP_RESPONSE = "http_response";
    public static final String HTTP_RESPONSE_BYTES = "http_response_bytes";
    public static final String HTTP_RESPONSE_CODE = "http_response_code";
    public static final String HTTP_URL = "http_url";
    public static final String HTTP_URL_CATEGORY = "http_url_category";
    public static final String HTTP_USER_AGENT = "http_user_agent";
    public static final String HTTP_USER_AGENT_ANALYZED = "http_user_agent_analyzed";
    public static final String HTTP_USER_AGENT_LENGTH = "http_user_agent_length";
    public static final String HTTP_USER_AGENT_NAME = "http_user_agent_name";
    public static final String HTTP_USER_AGENT_OS = "http_user_agent_os";
    public static final String HTTP_VERSION = "http_version";
    public static final String HTTP_XFF = "http_version";

    // Derived and Enriched Fields
    public static final String HTTP_URL_ANALYZED = "http_url_analyzed";
    public static final String HTTP_URL_LENGTH = "http_url_length";
}

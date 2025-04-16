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
package org.graylog2.plugin;

public enum DocsHelper {
    PAGE_SENDING_JSONPATH("getting_in_log_data/json_path_from_http_api_input.html"),
    PAGE_SENDING_IPFIXPATH("getting_in_log_data/ipfix_input.html"),
    PAGE_ES_CONFIGURATION("setting_up_graylog/server_configuration_settings_reference.htm#SearchBackendConfigurationProperties"),
    PAGE_ES_VERSIONS("downloading_and_installing_graylog/compatibility_matrix.htm"),
    REPORTING_HELP("interacting_with_your_log_data/reporting.html");

    private static final String SERVER = "https://go2docs.graylog.org/current";

    private final String path;

    DocsHelper(String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return SERVER + "/" + path;
    }
}

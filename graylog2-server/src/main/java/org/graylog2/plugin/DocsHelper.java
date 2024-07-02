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

import org.graylog2.shared.ServerVersion;

public enum DocsHelper {
    PAGE_SENDING_JSONPATH("getting_in_log_data/json_path_from_http_api_input.html"),
    PAGE_SENDING_IPFIXPATH("getting_in_log_data/ipfix_input.html"),
    PAGE_ES_CONFIGURATION("https://go2docs.graylog.org/current/setting_up_graylog/server.conf.html#OpenSearch"),
    PAGE_ES_VERSIONS("downloading_and_installing_graylog/installing_graylog.html#CompatibilityMatrix"),
    REPORTING_HELP("interacting_with_your_log_data/reporting.html");

    private static final String SERVER = "https://go2docs.graylog.org";

    private final String path;

    DocsHelper(String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return SERVER + "/" + version() + "/" + path;
    }

    private String version() {
        final var version = ServerVersion.VERSION.getVersion();
        if(version.toString().contains("SNAPSHOT")) {
            return "current";
        } else {
            return version.getMajorVersion() + "-" + version.getMinorVersion();
        }
    }

    public String toLink(String title) {
        return "<a href=\"" + this + "\" target=\"_blank\">" + title + "</a>";
    }
}

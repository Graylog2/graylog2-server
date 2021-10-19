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
    PAGE_SENDING_JSONPATH("json"),
    PAGE_SENDING_IPFIXPATH("ipfix-input"),
    PAGE_ES_CONFIGURATION("elasticsearch"),
    PAGE_ES_VERSIONS("elasticsearch#elasticsearch-versions");

    private static final String DOCS_URL = "https://docs.graylog.org/docs/";

    private final String path;

    DocsHelper(String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return DOCS_URL + path;
    }

    public String toLink(String title) {
        return "<a href=\"" + toString() + "\" target=\"_blank\">" + title + "</a>";
    }
}

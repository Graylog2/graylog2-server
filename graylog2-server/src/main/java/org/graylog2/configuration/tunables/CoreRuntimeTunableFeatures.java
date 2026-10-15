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
package org.graylog2.configuration.tunables;

import java.util.Set;

public final class CoreRuntimeTunableFeatures {
    public static final RuntimeTunableFeature SEARCH = new RuntimeTunableFeature("search", "Search", 100);
    public static final RuntimeTunableFeature MESSAGE_PROCESSORS = new RuntimeTunableFeature("message_processors", "Message Processors", 200);
    public static final RuntimeTunableFeature SIDECARS = new RuntimeTunableFeature("sidecars", "Sidecars", 300);
    public static final RuntimeTunableFeature EVENTS = new RuntimeTunableFeature("events", "Events", 400);
    public static final RuntimeTunableFeature URL_ALLOWLIST = new RuntimeTunableFeature("url_allowlist", "URL Allowlist", 500);
    public static final RuntimeTunableFeature DECORATORS = new RuntimeTunableFeature("decorators", "Decorators", 600);
    public static final RuntimeTunableFeature PERMISSIONS = new RuntimeTunableFeature("permissions", "Permissions", 700);
    public static final RuntimeTunableFeature PASSWORD_POLICY = new RuntimeTunableFeature("password_policy", "Password Policy", 800);
    public static final RuntimeTunableFeature USERS = new RuntimeTunableFeature("users", "Users", 900);
    public static final RuntimeTunableFeature MARKDOWN = new RuntimeTunableFeature("markdown", "Markdown", 1000);
    public static final RuntimeTunableFeature WELCOME_PAGE = new RuntimeTunableFeature("welcome_page", "Welcome Page", 1100);
    public static final RuntimeTunableFeature MCP = new RuntimeTunableFeature("mcp", "MCP", 1200);
    public static final RuntimeTunableFeature THREAT_INTELLIGENCE = new RuntimeTunableFeature("threat_intelligence", "Threat Intelligence Lookup", 1300);

    public static final Set<RuntimeTunableFeature> ALL = Set.of(SEARCH, MESSAGE_PROCESSORS, SIDECARS, EVENTS,
            URL_ALLOWLIST, DECORATORS, PERMISSIONS, PASSWORD_POLICY, USERS, MARKDOWN, WELCOME_PAGE, MCP, THREAT_INTELLIGENCE);

    private CoreRuntimeTunableFeatures() {
    }
}

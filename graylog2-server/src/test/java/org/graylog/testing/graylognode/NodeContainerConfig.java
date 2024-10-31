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
package org.graylog.testing.graylognode;

import org.apache.commons.lang.ArrayUtils;
import org.graylog.testing.completebackend.MavenProjectDirProvider;
import org.graylog.testing.completebackend.PluginJarsProvider;
import org.graylog2.storage.SearchVersion;
import org.testcontainers.containers.Network;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class NodeContainerConfig {

    static final int API_PORT = 9000;
    public static final int GELF_HTTP_PORT = 12201;
    static final int DEBUG_PORT = 5005;

    public final Network network;
    public final String mongoDbUri;
    public final String passwordSecret;
    public final String rootPasswordSha2;
    public final SearchVersion elasticsearchVersion;
    public final String elasticsearchUri;
    public final boolean enableDebugging;
    public final boolean skipPackaging;
    public final PluginJarsProvider pluginJarsProvider;
    public final MavenProjectDirProvider mavenProjectDirProvider;
    private final List<String> enabledFeatureFlags;
    public final Optional<String> proxiedRequestsTimeout;
    public final Map<String, String> configParams;

    public NodeContainerConfig(Network network,
                               String mongoDbUri,
                               final String passwordSecret,
                               final String rootPasswordSha2,
                               String elasticsearchUri,
                               SearchVersion elasticsearchVersion,
                               PluginJarsProvider pluginJarsProvider,
                               MavenProjectDirProvider mavenProjectDirProvider,
                               List<String> enabledFeatureFlags,
                               Map<String, String> configParams) {
        this.network = network;
        this.mongoDbUri = mongoDbUri;
        this.passwordSecret = passwordSecret;
        this.rootPasswordSha2 = rootPasswordSha2;
        this.elasticsearchUri = elasticsearchUri;
        this.elasticsearchVersion = elasticsearchVersion;
        this.enableDebugging = flagFromEnvVar("GRAYLOG_IT_DEBUG_SERVER");
        this.skipPackaging = flagFromEnvVar("GRAYLOG_IT_SKIP_PACKAGING");
        this.pluginJarsProvider = pluginJarsProvider;
        this.mavenProjectDirProvider = mavenProjectDirProvider;
        this.enabledFeatureFlags = enabledFeatureFlags == null ? Collections.emptyList() : enabledFeatureFlags;
        this.proxiedRequestsTimeout = stringFromEnvVar("GRAYLOG_IT_PROXIED_REQUESTS_TIMEOUT");
        this.configParams = configParams;
    }

    public static boolean flagFromEnvVar(String flagName) {
        String flag = System.getenv(flagName);
        return flag != null && flag.equalsIgnoreCase("true");
    }

    private static Optional<String> stringFromEnvVar(String flagName) {
        return Optional.ofNullable(System.getenv(flagName));
    }

    public Integer[] portsToExpose() {
        int[] allPorts = new int[] {GELF_HTTP_PORT};
        allPorts = ArrayUtils.add(allPorts, 0, API_PORT);

        if (enableDebugging) {
            allPorts = ArrayUtils.add(allPorts, 0, DEBUG_PORT);
        }

        return Arrays.stream(allPorts).boxed().toArray(Integer[]::new);
    }

    public List<String> getEnabledFeatureFlags() {
        return enabledFeatureFlags;
    }
}

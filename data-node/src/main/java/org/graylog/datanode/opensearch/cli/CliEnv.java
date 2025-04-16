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
package org.graylog.datanode.opensearch.cli;

import io.jsonwebtoken.lang.Collections;
import org.graylog2.shared.utilities.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class CliEnv {
    public static final String OPENSEARCH_PATH_CONF_ENV = "OPENSEARCH_PATH_CONF";
    public static final String OPENSEARCH_JAVA_HOME_ENV = "OPENSEARCH_JAVA_HOME";

    private final Map<String, String> env = new LinkedHashMap<>();

    /**
     * @param configDir config dir is mandatory and always has to be set.
     * @see #configDir(Path)
     */
    public CliEnv(Path configDir) {
        configDir(configDir);
    }

    /**
     * Opensearch CLI tools adapt configuration stored under OPENSEARCH_PATH_CONF env property.
     * This is why we always want to set this configPath for each CLI tool.
     */
    private CliEnv configDir(Path configDir) {
        this.env.put(OPENSEARCH_PATH_CONF_ENV, checkDir(configDir).toAbsolutePath().toString());
        return this;
    }

    /**
     * Set JVM that should be used to execute the binary. In typical case, we'd use the one that's bundled with this
     * opensearch distribution (by not providing any explicit JVM).
     * But if we are managing aarch64 distribution from a x64 build, we need to provide  our own JVM to do so,
     * otherwise the task fails.
     */
    public CliEnv javaHome(String javaHome) {
        this.env.put(OPENSEARCH_JAVA_HOME_ENV, checkDir(Path.of(javaHome)).toAbsolutePath().toString());
        return this;
    }

    private Path checkDir(Path dir) {
        if (!Files.isDirectory(dir)) {
            throw new IllegalArgumentException(StringUtils.f("%s is not a directory", dir));
        }
        return dir;
    }

    public Map<String, String> getEnv() {
        return Collections.immutable(this.env);
    }
}

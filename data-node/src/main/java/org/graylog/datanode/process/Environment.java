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
package org.graylog.datanode.process;

import org.apache.commons.exec.OS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

public record Environment(Map<String, String> env) {

    private static final Logger LOG = LoggerFactory.getLogger(Environment.class);

    private static final String JAVA_HOME_ENV = "JAVA_HOME";
    private static final String OPENSEARCH_JAVA_HOME_ENV = "OPENSEARCH_JAVA_HOME";
    public static final String OPENSEARCH_JAVA_OPTS_ENV = "OPENSEARCH_JAVA_OPTS";
    public static final String OPENSEARCH_PATH_CONF_ENV = "OPENSEARCH_PATH_CONF";

    public Environment(Map<String, String> env) {
        this.env = new HashMap<>(cleanEnvironment(env));
    }

    public Environment put(String key, String value) {
        this.env.put(key, value);
        return this;
    }

    /**
     * OPENSEARCH_JAVA_HOME is the first env property where opensearch binary is looking for java home. If we set this,
     * we can be sure that this JVM will be actually used, preventing users to override it.
     */
    public Environment withOpensearchJavaHome(Path javaHome) {
        if (OS.isFamilyMac()) {
            put(OPENSEARCH_JAVA_HOME_ENV, resolveMacJdk());
        } else {
            put(OPENSEARCH_JAVA_HOME_ENV, javaHome.toAbsolutePath().toString());
        }
        return this;
    }

    public Environment withOpensearchJavaOpts(List<String> javaOpts) {
        put(OPENSEARCH_JAVA_OPTS_ENV, String.join(" ", javaOpts));
        return this;
    }

    public Environment withOpensearchPathConf(Path opensearchConfPath) {
        put(OPENSEARCH_PATH_CONF_ENV, opensearchConfPath.toString());
        return this;
    }

    @Override
    public Map<String, String> env() {
        return Collections.unmodifiableMap(env);
    }

    private static Map<String, String> cleanEnvironment(Map<String, String> env) {
        return env.entrySet().stream()
                // Remove JAVA_HOME from environment because OpenSearch should use its bundled JVM.
                .filter(not(entry -> JAVA_HOME_ENV.equals(entry.getKey()))).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * as long as OpenSearch is not supported on macOS, we have to fix the jdk path if we want to
     * start the DataNode inside IntelliJ.
     */
    private String resolveMacJdk() {
        LOG.warn("MacOS is not supported by OpenSearch. Setting the system java home as OpenSearch java home.");
        // determine system jdk for OS jdk
        final ProcessBuilder builder = new ProcessBuilder("/usr/libexec/java_home");
        builder.redirectErrorStream(true);
        try {
            final Process process = builder.start();
            final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.defaultCharset()));
            var line = reader.readLine();
            if (line != null && Files.exists(Path.of(line))) {
                return line;
            } else {
                LOG.error("Output of '/usr/libexec/java_home' is not the jdk: {}", line);
            }
            // cleanup
            process.destroy();
            reader.close();
        } catch (IOException e) {
            LOG.error("Could not link jdk.app on macOS: {}", e.getMessage(), e);
        }
        return "";
    }
}


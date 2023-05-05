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
package org.graylog.datanode.management;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

public class Environment {

    private static final String JAVA_HOME_ENV = "JAVA_HOME";

    private final Map<String, String> env;
    private final Map<String, String> additionalVariables = new HashMap<>();

    public Environment(Map<String, String> env) {
        this.env = env;
    }

    public Environment put(String key, String value) {
        this.additionalVariables.put(key, value);
        return this;
    }

    public Map<String, String> getEnv() {
        Map<String, String> env = new HashMap<>();
        env.putAll(cleanEnvironment(env));
        env.putAll(additionalVariables);
        return Collections.unmodifiableMap(env);
    }

    private Map<String, String> cleanEnvironment(Map<String, String> env) {
        return env.entrySet().stream()
                // Remove JAVA_HOME from environment because OpenSearch should use its bundled JVM.
                .filter(not(entry -> JAVA_HOME_ENV.equals(entry.getKey())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}


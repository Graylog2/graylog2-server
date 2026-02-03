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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

class EnvironmentTest {

    @Test
    void testFiltering() {
        Map<String, String> userEnv = new LinkedHashMap<>();
        userEnv.put("USER", "test");
        userEnv.put("JAVA_HOME", "/path/to/jre");
        userEnv.put("OPENSEARCH_JAVA_HOME", "/path/to/jre");
        final Environment env = new Environment(userEnv);
        env.withOpensearchJavaHome(Path.of("/dist/opensearch/jdk"));

        Assertions.assertThat(env.env())
                .doesNotContainKey("JAVA_HOME")
                .containsKey("USER")
                .containsEntry("OPENSEARCH_JAVA_HOME", "/dist/opensearch/jdk");
    }
}

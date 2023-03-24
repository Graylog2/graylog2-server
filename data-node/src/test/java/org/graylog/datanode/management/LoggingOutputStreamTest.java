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

import org.apache.commons.exec.StreamPumper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

class LoggingOutputStreamTest {
    @Test
    void testConsumer() {

        final List<String> lines = new LinkedList<>();
        final LoggingOutputStream loggingOutputStream = new LoggingOutputStream(lines::add);

        String text = """
                first line
                second line
                third line
                """;
        final ByteArrayInputStream source = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));

        final StreamPumper pumper = new StreamPumper(source, loggingOutputStream);
        pumper.run();

        Assertions.assertThat(lines)
                .hasSize(3)
                .contains("first line", "second line", "third line");

    }
}

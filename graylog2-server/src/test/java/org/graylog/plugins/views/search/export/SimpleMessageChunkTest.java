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
package org.graylog.plugins.views.search.export;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.plugins.views.search.export.LinkedHashSetUtil.linkedHashSetOf;
import static org.graylog.plugins.views.search.export.TestData.simpleMessage;
import static org.graylog.plugins.views.search.export.TestData.simpleMessageChunk;

class SimpleMessageChunkTest {
    @Test
    void getsValuesInOrder() {
        Object[] msg1Values = {"2015-01-01 01:00:00.000", "source-1"};
        Object[] msg2Values = {"2015-01-02 01:00:00.000", "source-2"};
        SimpleMessageChunk sut = simpleMessageChunk("timestamp,source",
                msg1Values,
                msg2Values
        );

        assertThat(sut.getAllValuesInOrder()).containsExactly(msg1Values, msg2Values);
    }

    @Test
    void valuesInOrderContainsMissingFieldsAsNull() {
        SimpleMessage msg1 = simpleMessage("timestamp,source", new Object[]{"2015-01-01 01:00:00.000", "source-1"});
        SimpleMessage msg2 = simpleMessage("timestamp", new Object[]{"2015-01-02 01:00:00.000"});

        SimpleMessageChunk sut = SimpleMessageChunk.from(
                linkedHashSetOf("timestamp", "source"),
                msg1, msg2);

        assertThat(sut.getAllValuesInOrder()).containsExactly(
                new Object[]{"2015-01-01 01:00:00.000", "source-1"},
                new Object[]{"2015-01-02 01:00:00.000", null});
    }
}

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
package org.graylog.events.fields;

import com.google.common.collect.ImmutableSortedMap;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EventFieldNamesTest {

    @Test
    void sortsCaseInsensitively() {
        final Map<String, String> input = new LinkedHashMap<>();
        input.put("zulu", "1");
        input.put("Alpha", "2");
        input.put("mike", "3");

        assertThat(EventFieldNames.sorted(input).keySet())
                .containsExactly("Alpha", "mike", "zulu");
    }

    @Test
    void keepsBothKeysWhenTheyDifferOnlyInCase() {
        final Map<String, String> input = new LinkedHashMap<>();
        input.put("source", "lower");
        input.put("Source", "upper");

        final ImmutableSortedMap<String, String> result = EventFieldNames.sorted(input);

        assertThat(result).hasSize(2);
        assertThat(result.keySet()).containsExactly("Source", "source");
        assertThat(result).containsEntry("source", "lower");
        assertThat(result).containsEntry("Source", "upper");
    }

    @Test
    void returnsAnEmptyMapForAnEmptyInput() {
        assertThat(EventFieldNames.sorted(Map.of())).isEmpty();
    }

    @Test
    void isStableRegardlessOfInsertionOrder() {
        final Map<String, String> forwards = new LinkedHashMap<>();
        forwards.put("a", "1");
        forwards.put("b", "2");

        final Map<String, String> backwards = new LinkedHashMap<>();
        backwards.put("b", "2");
        backwards.put("a", "1");

        assertThat(EventFieldNames.sorted(forwards).keySet())
                .containsExactlyElementsOf(EventFieldNames.sorted(backwards).keySet());
    }
}

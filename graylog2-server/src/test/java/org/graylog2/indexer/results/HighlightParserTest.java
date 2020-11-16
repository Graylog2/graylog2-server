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
package org.graylog2.indexer.results;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class HighlightParserTest {
    @Test
    public void simpleHighlightAtStartOfString() throws Exception {
        final Map<String, List<String>> highlights = ImmutableMap.of(
                "message", ImmutableList.of("<em>last</em> message repeated 2 times")
        );

        final Multimap<String, Range<Integer>> result = HighlightParser.extractHighlightRanges(highlights);

        assertThat(result).isNotNull();
        assertThat(result.get("message")).isNotNull()
                .isNotEmpty()
                .containsExactly(Range.closed(0, 4));
    }

    @Test
    public void multipleHighlights() throws Exception {
        final Map<String, List<String>> highlights = ImmutableMap.of(
                "message", ImmutableList.of("/<em>usr</em>/sbin/cron[22390]: (root) CMD (/<em>usr</em>/libexec/atrun)"),
                "full_message", ImmutableList.of("<78>Aug 22 10:40:00 /<em>usr</em>/sbin/cron[22390]: (root) CMD (/<em>usr</em>/libexec/atrun)")
        );

        final Multimap<String, Range<Integer>> result = HighlightParser.extractHighlightRanges(highlights);

        assertThat(result).isNotNull();
        assertThat(result.get("message")).isNotNull()
                .isNotEmpty()
                .containsExactly(
                        Range.closed(1, 4),
                        Range.closed(36, 39)
                );
        assertThat(result.get("full_message")).isNotNull()
                .isNotEmpty()
                .containsExactly(
                        Range.closed(21, 24),
                        Range.closed(56, 59)
                );
    }

    @Test
    public void brokenHighlight() throws Exception {
        final Map<String, List<String>> highlights = ImmutableMap.of(
                "message", ImmutableList.of("/<em>usr</em>/sbin</em>/cron[22390]<em>: (root) CMD (/<em>usr</em>/libexec/atrun)")
        );

        final Multimap<String, Range<Integer>> result = HighlightParser.extractHighlightRanges(highlights);

        assertThat(result).isNotNull();
        assertThat(result.get("message")).isNotNull()
                .isNotEmpty()
                .containsExactly(
                        Range.closed(1, 4),
                        Range.closed(26, 48)
                );
    }

    @Test
    public void emptyHighlights() throws Exception {
        final Map<String, List<String>> highlights = ImmutableMap.of();

        final Multimap<String, Range<Integer>> result = HighlightParser.extractHighlightRanges(highlights);

        assertThat(result).isNotNull();
        assertThat(result.entries()).isEmpty();
    }

    @Test
    public void nullHighlights() throws Exception {
        final Multimap<String, Range<Integer>> result = HighlightParser.extractHighlightRanges(null);

        assertThat(result).isNotNull();
        assertThat(result.entries()).isEmpty();
    }
}
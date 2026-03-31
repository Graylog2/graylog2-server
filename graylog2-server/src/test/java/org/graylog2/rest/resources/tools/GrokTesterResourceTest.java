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
package org.graylog2.rest.resources.tools;

import org.graylog2.events.ClusterEventBus;
import org.graylog2.grok.GrokPattern;
import org.graylog2.grok.InMemoryGrokPatternService;
import org.graylog2.rest.resources.tools.responses.GrokTesterResponse;
import org.graylog2.shared.SuppressForbidden;
import org.graylog2.shared.bindings.GuiceInjectorHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.graylog2.shared.utilities.StringUtils.f;

public class GrokTesterResourceTest {
    static {
        GuiceInjectorHolder.createInjector(Collections.emptyList());
    }

    private GrokTesterResource resource;

    @BeforeEach
    @SuppressForbidden("Using Executors.newSingleThreadExecutor() is okay in tests")
    public void setUp() throws Exception {
        final ClusterEventBus clusterEventBus = new ClusterEventBus("cluster-event-bus", Executors.newSingleThreadExecutor());
        final InMemoryGrokPatternService grokPatternService = new InMemoryGrokPatternService(clusterEventBus);
        grokPatternService.save(GrokPattern.create("NUMBER", "[0-9]+"));
        grokPatternService.save(GrokPattern.create("INT", "(?:[+-]?(?:[0-9]+))"));
        grokPatternService.save(GrokPattern.create("YEAR", "(?:\\d\\d){1,2}"));
        grokPatternService.save(GrokPattern.create("MONTHNUM", "(?:0[1-9]|1[0-2])"));
        grokPatternService.save(GrokPattern.create("MONTHDAY", "(?:(?:0[1-9])|(?:[12][0-9])|(?:3[01])|[1-9])"));
        grokPatternService.save(GrokPattern.create("HOUR", "(?:2[0123]|[01]?[0-9])"));
        grokPatternService.save(GrokPattern.create("MINUTE", "(?:[0-5][0-9])"));
        grokPatternService.save(GrokPattern.create("SECOND", "(?:(?:[0-5]?[0-9]|60)(?:[:.,][0-9]+)?)"));
        grokPatternService.save(GrokPattern.create("TIME", "(?!<[0-9])%{HOUR}:%{MINUTE}(?::%{SECOND})(?![0-9])"));
        grokPatternService.save(GrokPattern.create("ISO8601_TIMEZONE", "(?:Z|[+-]%{HOUR}(?::?%{MINUTE}))"));
        grokPatternService.save(GrokPattern.create("ISO8601_SECOND", "(?:%{SECOND}|60)"));
        grokPatternService.save(GrokPattern.create("TIMESTAMP_ISO8601", "%{YEAR}-%{MONTHNUM}-%{MONTHDAY}[T ]%{HOUR}:?%{MINUTE}(?::?%{SECOND})?%{ISO8601_TIMEZONE}?"));
        grokPatternService.save(GrokPattern.create("LOGLEVEL", "([Aa]lert|ALERT|[Tt]race|TRACE|[Dd]ebug|DEBUG|[Nn]otice|NOTICE|[Ii]nfo|INFO|[Ww]arn(?:ing)?|WARN(?:ING)?|[Ee]rr(?:or)?|ERR(?:OR)?|[Cc]rit(?:ical)?|CRIT(?:ICAL)?|[Ff]atal|FATAL|[Ss]evere|SEVERE|EMERG(?:ENCY)?|[Ee]merg(?:ency)?)"));
        grokPatternService.save(GrokPattern.create("SPACE", "\\s*"));
        grokPatternService.save(GrokPattern.create("DATA", ".*?"));
        resource = new GrokTesterResource(grokPatternService);
    }

    @Test
    public void testGrokWithValidPatternAndMatch() {
        final GrokTesterResponse response = resource.grokTest("%{NUMBER}", "abc 1234", false);
        assertThat(response.matched()).isTrue();
        assertThat(response.pattern()).isEqualTo("%{NUMBER}");
        assertThat(response.string()).isEqualTo("abc 1234");
        assertThat(response.matches()).containsOnly(GrokTesterResponse.Match.create("NUMBER", "1234"));
        assertThat(response.errorMessage()).isNullOrEmpty();
    }

    @Test
    public void testGrokWithValidPatternAndNoMatch() {
        final GrokTesterResponse response = resource.grokTest("%{NUMBER}", "abc def", false);
        assertThat(response.matched()).isFalse();
        assertThat(response.pattern()).isEqualTo("%{NUMBER}");
        assertThat(response.string()).isEqualTo("abc def");
        assertThat(response.matches()).isEmpty();
        assertThat(response.errorMessage()).isNullOrEmpty();
    }

    @Test
    public void testGrokWithInvalidPattern() {
        final GrokTesterResponse response = resource.grokTest("%{NUMBER", "abc 1234", false);
        assertThat(response.matched()).isFalse();
        assertThat(response.pattern()).isEqualTo("%{NUMBER");
        assertThat(response.string()).isEqualTo("abc 1234");
        assertThat(response.errorMessage()).startsWith("Illegal repetition near index 2");
    }

    @Test
    public void testGrokWithMissingPattern() {
        final GrokTesterResponse response = resource.grokTest("%{FOOBAR} %{NUMBER}", "abc 1234", false);
        assertThat(response.matched()).isFalse();
        assertThat(response.pattern()).isEqualTo("%{FOOBAR} %{NUMBER}");
        assertThat(response.string()).isEqualTo("abc 1234");
        assertThat(response.errorMessage()).isEqualTo("No definition for key 'FOOBAR' found, aborting");
    }

    @Test
    public void testGrokWithEmptyPattern() {
        final GrokTesterResponse response = resource.grokTest("", "abc 1234", false);
        assertThat(response.matched()).isFalse();
        assertThat(response.pattern()).isEqualTo("");
        assertThat(response.string()).isEqualTo("abc 1234");
        assertThat(response.errorMessage()).isEqualTo("{pattern} should not be empty or null");
    }

    @Test
    public void testGrokWithEmptyTestString() {
        final GrokTesterResponse response = resource.grokTest("%{NUMBER}", "", false);
        assertThat(response.matched()).isFalse();
        assertThat(response.pattern()).isEqualTo("%{NUMBER}");
        assertThat(response.string()).isEqualTo("");
        assertThat(response.errorMessage()).isNullOrEmpty();
    }

    // See: https://github.com/Graylog2/graylog-plugin-enterprise/issues/13717
    // The pattern (?<message>(.|\r|\n)*) uses a nested quantifier that causes deep recursion
    // in Java's regex engine on long input, resulting in a StackOverflowError.
    @Test
    public void testGrokWithNestedQuantifierCausesStackOverflow() {
        final String pattern = "%{TIMESTAMP_ISO8601:logtime}%{SPACE:UNWANTED}%{LOGLEVEL:loglevel}%{SPACE:UNWANTED}" +
                "\\[%{DATA:loggername}\\]%{SPACE:UNWANTED}(?<message>(.|\r|\n)*)";
        final String sampleData = buildLongLogLine();

        final GrokTesterResponse response = resource.grokTest(pattern, sampleData, false);
        assertThat(response.matched()).isFalse();
        assertThat(response.errorMessage()).contains("stack overflow");
    }

    // See: https://github.com/Graylog2/graylog-plugin-enterprise/issues/13717
    // The workaround replaces (?<message>(.|\r|\n)*) with (?<message>[\s\S]*) which uses a
    // character class instead of a nested group, avoiding deep recursion.
    @Test
    public void testGrokWithCharacterClassWorkaroundDoesNotOverflow() {
        final String pattern = "%{TIMESTAMP_ISO8601:logtime}%{SPACE:UNWANTED}%{LOGLEVEL:loglevel}%{SPACE:UNWANTED}" +
                "\\[%{DATA:loggername}\\]%{SPACE:UNWANTED}(?<message>[\\s\\S]*)";
        final String sampleData = buildLongLogLine();

        assertThatNoException().isThrownBy(() -> {
            final GrokTesterResponse response = resource.grokTest(pattern, sampleData, false);
            assertThat(response.matched()).isTrue();
            assertThat(response.errorMessage()).isNullOrEmpty();
            assertThat(response.matches()).extracting("name").contains("logtime", "loglevel", "loggername", "message");
        });
    }

    // See: https://github.com/Graylog2/graylog-plugin-enterprise/issues/13717
    // Simplest fix: drop the trailing message capture entirely. The customer only needs
    // logtime, loglevel, and loggername — the message field is already on the log entry.
    @Test
    public void testGrokWithoutTrailingCaptureDoesNotOverflow() {
        final String pattern = "%{TIMESTAMP_ISO8601:logtime}%{SPACE:UNWANTED}%{LOGLEVEL:loglevel}%{SPACE:UNWANTED}" +
                "\\[%{DATA:loggername}\\]";
        final String sampleData = buildLongLogLine();

        final GrokTesterResponse response = resource.grokTest(pattern, sampleData, false);
        assertThat(response.matched()).isTrue();
        assertThat(response.errorMessage()).isNullOrEmpty();
        assertThat(response.matches()).extracting("name").contains("logtime", "loglevel", "loggername");
    }

    /**
     * Builds a log line similar to the one in the customer report — a long Graylog server log entry
     * with many stream IDs that is large enough to trigger stack overflow with nested quantifiers.
     */
    private static String buildLongLogLine() {
        final String streamIds = IntStream.range(0, 200)
                .mapToObj(i -> f("5e3957%04d94e3e27%06x", i, i * 7))
                .collect(Collectors.joining(", "));
        return "2026-03-30T09:33:57.653+02:00 INFO  [RebuildIndexRangesJob] Created ranges for index logmonitor_21636: " +
                "MongoIndexRange{id=null, indexName=logmonitor_21636, begin=1975-08-16T05:23:51.326Z, " +
                "end=2026-03-20T06:58:54.421Z, calculatedAt=2026-03-30T07:33:53.631Z, " +
                "calculationDuration=4015, streamIds=[" + streamIds + "]}";
    }
}

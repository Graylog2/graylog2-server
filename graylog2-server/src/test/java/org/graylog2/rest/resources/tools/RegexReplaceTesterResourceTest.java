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

import jakarta.ws.rs.BadRequestException;
import org.graylog2.rest.models.tools.requests.RegexReplaceTestRequest;
import org.graylog2.rest.models.tools.responses.RegexReplaceTesterResponse;
import org.graylog2.shared.bindings.GuiceInjectorHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class RegexReplaceTesterResourceTest {

    private RegexReplaceTesterResource toTest;

    public RegexReplaceTesterResourceTest() {
        GuiceInjectorHolder.createInjector(Collections.emptyList());
    }

    @BeforeEach
    public void setUp() {
        toTest = new RegexReplaceTesterResource(new SafePattern(Clock.systemUTC()));
    }

    @Test
    public void regexTesterReturns400WithInvalidRegularExpression() {
        assertThrows(BadRequestException.class, () -> toTest.regexTester("?*foo", "x", false, "test"));
    }

    @Test
    public void regexTesterReturnsMatchedResponseWhenPatternMatches() {
        final RegexReplaceTesterResponse response = toTest.regexTester("([a-z]+)", "X", false, "test");
        assertThat(response.matched()).isTrue();
        assertThat(response.regex()).isEqualTo("([a-z]+)");
        assertThat(response.string()).isEqualTo("test");
        assertThat(response.replacement()).isEqualTo("X");
        assertThat(response.match()).isNotNull();
        assertThat(response.match().match()).isEqualTo("X");
    }

    @Test
    public void regexTesterReturnsUnmatchedResponseWhenPatternDoesNotMatch() {
        final RegexReplaceTesterResponse response = toTest.regexTester("([0-9]+)", "X", false, "test");
        assertThat(response.matched()).isFalse();
        assertThat(response.match()).isNull();
    }

    @Test
    public void testRegexReturns400WithInvalidRegularExpression() {
        assertThrows(BadRequestException.class, () ->
                toTest.testRegex(RegexReplaceTestRequest.create("test", "?*foo", "X", false)));
    }

    @Test
    public void testRegexReturnsMatchedResponseWhenPatternMatches() {
        final RegexReplaceTesterResponse response = toTest.testRegex(
                RegexReplaceTestRequest.create("test", "([a-z]+)", "X", false));
        assertThat(response.matched()).isTrue();
        assertThat(response.match()).isNotNull();
    }

    @Test
    public void emptyReplacementDefaultsToGroup1MirroringExtractorBehaviour() {
        // RegexReplaceExtractor treats empty replacement as "$1" (DEFAULT_REPLACE_VALUE).
        // Using pattern "order-(\d+)" makes the difference observable:
        //   replacement ""    would give "" (whole match removed)
        //   replacement "$1"  gives "42"  (whole match replaced by captured group)
        final RegexReplaceTesterResponse response = toTest.regexTester("order-(\\d+)", "", false, "order-42");
        assertThat(response.matched()).isTrue();
        assertThat(response.match().match()).isEqualTo("42");
    }

    @Test
    public void regexTesterReturns400WhenReplacementReferencesNonExistentGroup() {
        // $9 refers to a capture group that does not exist — IndexOutOfBoundsException from Matcher
        assertThrows(BadRequestException.class,
                () -> toTest.regexTester("(\\d+)", "$9", false, "order-42"));
    }
}

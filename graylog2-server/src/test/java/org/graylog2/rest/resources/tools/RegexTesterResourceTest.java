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

import org.graylog2.rest.models.tools.requests.RegexTestRequest;
import org.graylog2.rest.models.tools.responses.RegexTesterResponse;
import org.graylog2.rest.models.tools.responses.RegexValidationResponse;
import org.graylog2.shared.bindings.GuiceInjectorHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.BadRequestException;

import java.util.Collections;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class RegexTesterResourceTest {

    private RegexTesterResource resource;

    public RegexTesterResourceTest() {
        GuiceInjectorHolder.createInjector(Collections.emptyList());
    }

    @BeforeEach
    public void setUp() throws Exception {
        resource = new RegexTesterResource();
    }

    @Test
    public void regexTesterReturns400WithInvalidRegularExpression() {
        Throwable exception = assertThrows(BadRequestException.class, () ->
            resource.regexTester("?*foo", "test"));
        org.hamcrest.MatcherAssert.assertThat(exception.getMessage(), containsString("Invalid regular expression: Dangling meta character '?' near index 0"));
    }

    @Test
    public void regexTesterReturnsValidResponseIfRegExMatches() throws Exception {
        final RegexTesterResponse response = resource.regexTester("([a-z]+)", "test");
        assertThat(response.matched()).isTrue();
        assertThat(response.regex()).isEqualTo("([a-z]+)");
        assertThat(response.string()).isEqualTo("test");
        assertThat(response.match()).isEqualTo(RegexTesterResponse.Match.create("test", 0, 4));
    }

    @Test
    public void regexTesterReturnsValidResponseIfRegExDoesNotMatch() throws Exception {
        final RegexTesterResponse response = resource.regexTester("([0-9]+)", "test");
        assertThat(response.matched()).isFalse();
        assertThat(response.regex()).isEqualTo("([0-9]+)");
        assertThat(response.string()).isEqualTo("test");
        assertThat(response.match()).isNull();
    }

    @Test
    public void testRegexReturns400WithInvalidRegularExpression() {
        Throwable exception = assertThrows(BadRequestException.class, () ->
            resource.testRegex(RegexTestRequest.create("test", "?*foo")));
        org.hamcrest.MatcherAssert.assertThat(exception.getMessage(), containsString("Invalid regular expression: Dangling meta character '?' near index 0"));
    }

    @Test
    public void testRegexReturnsValidResponseIfRegExMatches() throws Exception {
        final RegexTesterResponse response = resource.testRegex(RegexTestRequest.create("test", "([a-z]+)"));
        assertThat(response.matched()).isTrue();
        assertThat(response.regex()).isEqualTo("([a-z]+)");
        assertThat(response.string()).isEqualTo("test");
        assertThat(response.match()).isEqualTo(RegexTesterResponse.Match.create("test", 0, 4));
    }

    @Test
    public void testRegexReturnsValidResponseIfRegExDoesNotMatch() throws Exception {
        final RegexTesterResponse response = resource.testRegex(RegexTestRequest.create("test", "([0-9]+)"));
        assertThat(response.matched()).isFalse();
        assertThat(response.regex()).isEqualTo("([0-9]+)");
        assertThat(response.string()).isEqualTo("test");
        assertThat(response.match()).isNull();
    }

    @Test
    public void testValidateValidRegex() {
        final RegexValidationResponse response = resource.validateRegex(".*");
        assertThat(response.regex()).isEqualTo(".*");
        assertThat(response.isValid()).isTrue();
        assertThat(response.validationMessage().isPresent()).isFalse();
    }

    @Test
    public void testValidateInvalidRegex() {
        final RegexValidationResponse response = resource.validateRegex("?*foo");
        assertThat(response.regex()).isEqualTo("?*foo");
        assertThat(response.isValid()).isFalse();
        assertThat(response.validationMessage().isPresent()).isTrue();
        assertThat(response.validationMessage().get()).isNotBlank();
    }
}

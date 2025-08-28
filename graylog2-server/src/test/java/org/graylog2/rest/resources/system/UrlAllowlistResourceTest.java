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
package org.graylog2.rest.resources.system;

import org.graylog2.rest.models.system.urlallowlist.AllowlistRegexGenerationRequest;
import org.graylog2.rest.models.system.urlallowlist.AllowlistRegexGenerationResponse;
import org.graylog2.system.urlallowlist.RegexHelper;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.assertj.core.api.Assertions.assertThat;

public class UrlAllowlistResourceTest {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @InjectMocks
    UrlAllowlistResource urlAllowlistResource;

    @Spy
    RegexHelper regexHelper;

    @Test
    public void generateRegexForTemplate() {
        final AllowlistRegexGenerationRequest request =
                AllowlistRegexGenerationRequest.create("https://example.com/api/lookup?key=${key}", "${key}");
        final AllowlistRegexGenerationResponse response = urlAllowlistResource.generateRegex(request);
        assertThat(response.regex()).isNotBlank();
    }

    @Test
    public void generateRegexForUrl() {
        final AllowlistRegexGenerationRequest request =
                AllowlistRegexGenerationRequest.create("https://example.com/api/lookup", null);
        final AllowlistRegexGenerationResponse response = urlAllowlistResource.generateRegex(request);
        assertThat(response.regex()).isNotBlank();
    }
}

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
package org.graylog2.security.html;


import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HTMLSanitizerConverterTest {
    private static final String HTML_AND_MARKDOWN_FILE = "html-and-markdown.md";

    @Test
    void testConvert() throws IOException {
        final String markdownHtmlString = IOUtils.toString(Objects.requireNonNull(
                getClass().getResourceAsStream(HTML_AND_MARKDOWN_FILE)), StandardCharsets.UTF_8);
        final String result = new HTMLSanitizerConverter().convert(markdownHtmlString);
        final Set<String> forbiddenElements = Set.of("script", "form", "label", "input", "br", "button", "iframe", "footer", "<", ">");
        forbiddenElements.forEach(s -> assertFalse(result.contains(s)));
        final Set<String> markdownStrings = Set.of("Short Markdown Example", "Introduction", "List", "Item 2");
        markdownStrings.forEach(s -> assertTrue(result.contains(s)));
    }
}

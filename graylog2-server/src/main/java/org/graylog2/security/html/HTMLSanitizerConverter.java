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

import com.fasterxml.jackson.databind.util.StdConverter;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

/**
 * Jackson converter that removes all HTML elements from a specified string.
 * Uses the OWASP Java HTML Sanitizer library, which does not allow any HTML
 * elements by default.
 *
 * @see <a href="https://github.com/OWASP/java-html-sanitizer">OWASP Java HTML Sanitizer</a>
 */
public class HTMLSanitizerConverter extends StdConverter<String, String> {
    private static final PolicyFactory POLICY = new HtmlPolicyBuilder().toFactory();

    @Override
    public String convert(String input) {
        return POLICY.sanitize(input);
    }
}

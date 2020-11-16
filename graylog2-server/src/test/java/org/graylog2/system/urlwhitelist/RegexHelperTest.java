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
package org.graylog2.system.urlwhitelist;

import org.junit.Test;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

public class RegexHelperTest {
    RegexHelper regexHelper = new RegexHelper();

    @Test
    public void createRegexForTemplateUrl() {
        String url = "https://example.com/api/lookup?key=message_key&a=b&c=message_key&e=f";
        String template = "https://example.com/api/lookup?key=${key}&a=b&c=${key}&e=f";
        String expected = "^\\Qhttps://example.com/api/lookup?key=\\E.*?\\Q&a=b&c=\\E.*?\\Q&e=f\\E$";
        String got = regexHelper.createRegexForUrlTemplate(template, "${key}");
        assertThat(got).isEqualTo(expected);
        Pattern compiled = Pattern.compile(got, Pattern.DOTALL);
        assertThat(compiled.matcher(url).find()).isTrue();
    }

    @Test
    public void create() {
        String url = "https://example.com/api/lookup";
        String expected = "^\\Qhttps://example.com/api/lookup\\E$";
        String got = regexHelper.createRegexForUrl(url);
        assertThat(got).isEqualTo(expected);
        Pattern compiled = Pattern.compile(got, Pattern.DOTALL);
        assertThat(compiled.matcher(url).find()).isTrue();
    }
}

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
package org.graylog2.configuration.converters;

import com.github.joschi.jadconfig.Converter;
import com.github.zafarkhaja.semver.Version;
import org.graylog2.storage.SearchVersion;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MajorVersionConverterTest {

    private static final Converter<SearchVersion> converter = new MajorVersionConverter();

    @Test
    void convertSimpleNumber() {
        final SearchVersion version = converter.convertFrom("7");
        assertThat(version).isEqualTo(SearchVersion.elasticsearch("7.0.0"));
    }

    @Test
    void convertEncodedValue() {
        final SearchVersion version = converter.convertFrom("OPENSEARCH:1.2.0");
        assertThat(version).isEqualTo(SearchVersion.create(SearchVersion.Distribution.OPENSEARCH, Version.valueOf("1.2.0")));
    }

    @Test
    void testConvertToString() {
        final String converted = converter.convertTo(SearchVersion.create(SearchVersion.Distribution.OPENSEARCH, Version.valueOf("1.2.0")));
        assertThat(converted).isEqualTo("OPENSEARCH:1.2.0");
    }
}

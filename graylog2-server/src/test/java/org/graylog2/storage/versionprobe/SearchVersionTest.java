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
package org.graylog2.storage.versionprobe;

import org.graylog2.indexer.ElasticsearchException;
import org.graylog2.plugin.Version;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

class SearchVersionTest {

    @Test
    void major() {
        final SearchVersion searchVersion = SearchVersion.create(SearchVersion.Distribution.ELASTICSEARCH, ver("5.1.4"));
        assertThat(searchVersion.major().version().toString()).isEqualTo("5.0.0");
    }

    @Test
    void satisfies() {
        final SearchVersion searchVersion = SearchVersion.create(SearchVersion.Distribution.ELASTICSEARCH, ver("5.1.4"));
        assertThat(searchVersion.satisfies(SearchVersion.Distribution.ELASTICSEARCH, "^5.0.0")).isTrue();
        assertThat(searchVersion.satisfies(SearchVersion.Distribution.ELASTICSEARCH, "^4.0.0")).isFalse();
        assertThat(searchVersion.satisfies(SearchVersion.Distribution.OPENSEARCH, "^5.0.0")).isFalse();
    }

    @Test
    void testParseVersion() {
        assertThatThrownBy(() -> SearchVersion.parseVersion("v1")).isInstanceOfAny(ElasticsearchException.class);
        assertThatThrownBy(() -> SearchVersion.parseVersion("1.2.x")).isInstanceOfAny(ElasticsearchException.class);
    }

    private Version ver(final String version) {
        return new Version(com.github.zafarkhaja.semver.Version.valueOf(version));
    }
}

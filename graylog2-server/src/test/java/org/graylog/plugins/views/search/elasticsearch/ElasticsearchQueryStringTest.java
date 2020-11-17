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
package org.graylog.plugins.views.search.elasticsearch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ElasticsearchQueryStringTest {
    private ElasticsearchQueryString create(String queryString) {
        return ElasticsearchQueryString.builder().queryString(queryString).build();
    }

    @Test
    void concatenatingTwoEmptyStringsReturnsEmptyString() {
        assertThat(create("").concatenate(create("")).queryString()).isEmpty();
    }

    @Test
    void concatenatingNonEmptyStringWithEmptyStringReturnsFirst() {
        assertThat(create("_exists_:nf_version").concatenate(create("")).queryString()).isEqualTo("_exists_:nf_version");
    }

    @Test
    void concatenatingEmptyStringWithNonEmptyStringReturnsSecond() {
        assertThat(create("").concatenate(create("_exists_:nf_version")).queryString()).isEqualTo("_exists_:nf_version");
    }

    @Test
    void concatenatingTwoNonEmptyStringsReturnsAppendedQueryString() {
        assertThat(create("nf_bytes>200").concatenate(create("_exists_:nf_version")).queryString()).isEqualTo("nf_bytes>200 AND _exists_:nf_version");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            " ",
            "\n",
            "*",
            " *"
    })
    void detectsIfItsEmpty(String queryString) {
        ElasticsearchQueryString sut = ElasticsearchQueryString.builder().queryString(queryString).build();

        assertThat(sut.isEmpty()).isTrue();
    }
}

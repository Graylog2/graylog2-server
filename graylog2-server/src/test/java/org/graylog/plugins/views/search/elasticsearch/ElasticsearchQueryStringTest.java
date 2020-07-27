/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
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

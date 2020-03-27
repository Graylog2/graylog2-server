package org.graylog.plugins.views.search.elasticsearch;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ElasticsearchQueryStringTest {
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
}

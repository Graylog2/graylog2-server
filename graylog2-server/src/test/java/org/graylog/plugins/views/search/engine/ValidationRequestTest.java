package org.graylog.plugins.views.search.engine;

import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.Parameter;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationRequestTest {

    @Test
    void combineQueryFilterQueryOnly() throws InvalidRangeParametersException {
        final String q = builder()
                .query(ElasticsearchQueryString.of("foo:bar"))
                .build()
                .getCombinedQueryWithFilter();

        assertThat(q).isEqualTo("foo:bar");
    }

    @Test
    void combineQueryFilterBoth() throws InvalidRangeParametersException {
        final String q = builder()
                .query(ElasticsearchQueryString.of("foo:bar"))
                .filter(ElasticsearchQueryString.of("lorem:ipsum"))
                .build()
                .getCombinedQueryWithFilter();

        assertThat(q).isEqualTo("foo:bar AND lorem:ipsum");
    }

    private ValidationRequest.Builder builder() throws InvalidRangeParametersException {
        return ValidationRequest.builder()
                .timerange(RelativeRange.create(300))
                .streams(Collections.emptySet())
                .parameters(ImmutableSet.<Parameter>builder().build());
    }
}

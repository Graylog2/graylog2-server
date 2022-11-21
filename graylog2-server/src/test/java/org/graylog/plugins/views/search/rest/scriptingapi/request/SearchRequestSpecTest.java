package org.graylog.plugins.views.search.rest.scriptingapi.request;

import org.graylog2.plugin.indexer.searches.timeranges.KeywordRange;
import org.junit.jupiter.api.Test;

import javax.ws.rs.BadRequestException;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.plugins.views.search.rest.scriptingapi.request.SearchRequestSpec.DEFAULT_TIMERANGE;
import static org.junit.jupiter.api.Assertions.*;

class SearchRequestSpecTest {


    @Test
    void throwsBadRequestExceptionOnNullGroups() {
        assertThrows(BadRequestException.class, () -> SearchRequestSpec.fromSimpleParams("*",
                Set.of(),
                "42d",
                null,
                List.of("avg:joe")));
    }

    @Test
    void throwsBadRequestExceptionOnEmptyGroups() {
        assertThrows(BadRequestException.class, () -> SearchRequestSpec.fromSimpleParams("*",
                Set.of(),
                "42d",
                List.of(),
                List.of("avg:joe")));
    }

    @Test
    void throwsBadRequestExceptionOnWrongMetricFormat() {
        assertThrows(BadRequestException.class, () -> SearchRequestSpec.fromSimpleParams("*",
                Set.of(),
                "42d",
                List.of("http_method"),
                List.of("avg:joe", "ayayayayay!")));
    }

    @Test
    void usesProperDefaults() {
        SearchRequestSpec searchRequestSpec = SearchRequestSpec.fromSimpleParams(null,
                null,
                null,
                List.of("http_method"),
                null);

        assertThat(searchRequestSpec).isEqualTo(new SearchRequestSpec(
                        "*",
                        Set.of(),
                        DEFAULT_TIMERANGE,
                        List.of(new Grouping("http_method")),
                        List.of(new Metric(null, "count", null))
                )
        );

        searchRequestSpec = SearchRequestSpec.fromSimpleParams(null,
                null,
                null,
                List.of("http_method"),
                List.of());

        assertThat(searchRequestSpec).isEqualTo(new SearchRequestSpec(
                        "*",
                        Set.of(),
                        DEFAULT_TIMERANGE,
                        List.of(new Grouping("http_method")),
                        List.of(new Metric(null, "count", null))
                )
        );
    }

    @Test
    void createsProperRequestSpec() {
        final SearchRequestSpec searchRequestSpec = SearchRequestSpec.fromSimpleParams("http_method:GET",
                Set.of("000000000000000000000001"),
                "1d",
                List.of("http_method", "controller"),
                List.of("avg:took_ms"));

        assertThat(searchRequestSpec).isEqualTo(new SearchRequestSpec(
                        "http_method:GET",
                        Set.of("000000000000000000000001"),
                        KeywordRange.create("last 1 day", "UTC"),
                        List.of(new Grouping("http_method"), new Grouping("controller")),
                        List.of(new Metric("took_ms", "avg", null))
                )
        );
    }
}

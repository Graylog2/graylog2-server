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
package org.graylog.plugins.views.search.elasticsearch.searchtypes;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.revinate.assertj.json.JsonPathAssert;
import io.searchbox.core.SearchResult;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.QueryResult;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.ESGeneratedQueryContext;
import org.graylog.plugins.views.search.elasticsearch.ESQueryDecorator;
import org.graylog.plugins.views.search.elasticsearch.ESQueryDecorators;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.searchtypes.MessageList;
import org.graylog2.Configuration;
import org.graylog2.decorators.DecoratorProcessor;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.junit.Test;

import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ESMessageListTest {

    @Test
    public void includesCustomNameInResultIfPresent() {
        final ESMessageList esMessageList = new ESMessageList(new ESQueryDecorators(Collections.emptySet()));
        final MessageList messageList = someMessageList().toBuilder().name("customResult").build();

        final SearchResult result = new MockSearchResult(Collections.emptyList(), (long)0);

        final SearchType.Result searchTypeResult = esMessageList.doExtractResult(null, someQuery(), messageList, result, null, null);

        assertThat(searchTypeResult.name()).contains("customResult");
    }

    @Test
    public void usesHighlightingIfActivatedInConfig() {
        MessageList messageList = someMessageList();

        ESGeneratedQueryContext context = generateQueryPartFor(messageList, configWithHighlighting(true));

        assertThat(context.searchSourceBuilder(messageList).highlighter()).isNotNull();
    }

    @Test
    public void doesNotUseHighlightingIfDeactivatedInConfig() {
        MessageList messageList = someMessageList();

        ESGeneratedQueryContext context = generateQueryPartFor(messageList, configWithHighlighting(false));

        assertThat(context.searchSourceBuilder(messageList).highlighter())
                .as("there should be no highlighter configured")
                .isNull();
    }

    @Test
    public void appliesDecoratorsToQueryStringIfHighlightingActivated() {
        final ESQueryDecorator esQueryDecorator = (String queryString, SearchJob job, Query query, Set<QueryResult> results) -> "Foobar!";

        final MessageList messageList = someMessageList();

        ESGeneratedQueryContext queryContext = generateQueryPartFor(messageList, configWithHighlighting(true), Collections.singleton(esQueryDecorator));

        final DocumentContext doc = JsonPath.parse(queryContext.searchSourceBuilder(messageList).toString());
        JsonPathAssert.assertThat(doc).jsonPathAsString("$.highlight.highlight_query.query_string.query").isEqualTo("Foobar!");
    }

    private Query someQuery() {
        return Query.builder()
                .id("deadbeef")
                .query(ElasticsearchQueryString.builder()
                        .queryString("Something else")
                        .build())
                .timerange(someTimeRange())
                .searchTypes(Collections.emptySet())
                .build();
    }

    private RelativeRange someTimeRange() {
        try {
            return RelativeRange.create(300);
        } catch (InvalidRangeParametersException e) {
            throw new RuntimeException("invalid query time range spec");
        }
    }

    private MessageList someMessageList() {
        return MessageList.builder()
                .id("amessagelist")
                .limit(100)
                .offset(0)
                .build();
    }

    private ESGeneratedQueryContext generateQueryPartFor(MessageList messageList, Configuration config) {
        return generateQueryPartFor(messageList, config, Collections.emptySet());
    }

    private ESGeneratedQueryContext generateQueryPartFor(MessageList messageList, Configuration config, Set<ESQueryDecorator> decorators) {

        ESMessageList sut = new ESMessageList(
                new ESQueryDecorators(decorators),
                new DecoratorProcessor.Fake(),
                Collections.emptyMap(),
                config);

        ESGeneratedQueryContext context = mock(ESGeneratedQueryContext.class);

        when(context.searchSourceBuilder(messageList)).thenReturn(new SearchSourceBuilder());

        sut.doGenerateQueryPart(mock(SearchJob.class), someQuery(), messageList, context);

        return context;
    }

    private Configuration configWithHighlighting(boolean highlightingValue) {
        Configuration configuration = mock(Configuration.class);
        when(configuration.isAllowHighlighting()).thenReturn(highlightingValue);
        return configuration;
    }
}

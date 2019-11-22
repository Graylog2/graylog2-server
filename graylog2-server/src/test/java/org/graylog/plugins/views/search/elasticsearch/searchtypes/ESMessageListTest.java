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
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.junit.Test;

import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ESMessageListTest {
    @Test
    public void executesQueryDecoratorsOnQueryString() throws Exception {
        final ESQueryDecorator esQueryDecorator = (String queryString, SearchJob job, Query query, Set<QueryResult> results) -> "Foobar!";
        final ESMessageList esMessageList = new ESMessageList(new ESQueryDecorators(Collections.singleton(esQueryDecorator)));

        final Query query = Query.builder()
                .id("deadbeef")
                .query(ElasticsearchQueryString.builder()
                        .queryString("Something else")
                        .build())
                .timerange(RelativeRange.create(300))
                .searchTypes(Collections.emptySet())
                .build();
        final SearchJob searchJob = mock(SearchJob.class);
        final MessageList messageList = MessageList.builder()
                .id("amessagelist")
                .limit(100)
                .offset(0)
                .build();
        final ESGeneratedQueryContext esGeneratedQueryContext = mock(ESGeneratedQueryContext.class);
        final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        when(esGeneratedQueryContext.searchSourceBuilder(eq(messageList))).thenReturn(searchSourceBuilder);

        esMessageList.doGenerateQueryPart(searchJob, query, messageList, esGeneratedQueryContext);

        final DocumentContext doc = JsonPath.parse(searchSourceBuilder.toString());
        JsonPathAssert.assertThat(doc).jsonPathAsString("$.highlight.highlight_query.query_string.query").isEqualTo("Foobar!");
    }

    @Test
    public void addsNameToResults() {
        final ESMessageList esMessageList = new ESMessageList(new ESQueryDecorators(Collections.emptySet()));
        final MessageList messageList = MessageList.builder()
                .id("amessagelist")
                .name("customResult")
                .limit(100)
                .offset(0)
                .build();

        final SearchResult result = new MockSearchResult(Collections.emptyList(), (long)0);

        final SearchType.Result searchTypeResult = esMessageList.doExtractResult(null, null, messageList, result, null, null);

        assertThat(searchTypeResult.name()).contains("customResult");
    }
}

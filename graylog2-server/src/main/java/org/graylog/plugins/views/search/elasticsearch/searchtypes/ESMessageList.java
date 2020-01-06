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

import com.google.common.annotations.VisibleForTesting;
import io.searchbox.core.SearchResult;
import io.searchbox.core.search.aggregation.MetricAggregation;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.ESGeneratedQueryContext;
import org.graylog.plugins.views.search.elasticsearch.ESQueryDecorators;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.searchtypes.MessageList;
import org.graylog.plugins.views.search.searchtypes.Sort;
import org.graylog2.Configuration;
import org.graylog2.decorators.Decorator;
import org.graylog2.decorators.DecoratorProcessor;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.decorators.SearchResponseDecorator;
import org.graylog2.rest.models.messages.responses.ResultMessageSummary;
import org.graylog2.rest.resources.search.responses.SearchResponse;
import org.joda.time.DateTime;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.MoreObjects.firstNonNull;

public class ESMessageList implements ESSearchTypeHandler<MessageList> {
    private final ESQueryDecorators esQueryDecorators;
    private final DecoratorProcessor decoratorProcessor;
    private final Map<String, SearchResponseDecorator.Factory> searchResponseDecorators;
    private final Configuration configuration;

    @Inject
    public ESMessageList(ESQueryDecorators esQueryDecorators,
                         DecoratorProcessor decoratorProcessor,
                         Map<String, SearchResponseDecorator.Factory> searchResponseDecorators,
                         Configuration configuration) {
        this.esQueryDecorators = esQueryDecorators;
        this.decoratorProcessor = decoratorProcessor;
        this.searchResponseDecorators = searchResponseDecorators;
        this.configuration = configuration;
    }

    @VisibleForTesting
    public ESMessageList(ESQueryDecorators esQueryDecorators) {
        this(esQueryDecorators, new DecoratorProcessor.Fake(), Collections.emptyMap(), new Configuration());
    }

    @Override
    public void doGenerateQueryPart(SearchJob job, Query query, MessageList messageList, ESGeneratedQueryContext queryContext) {

        final SearchSourceBuilder searchSourceBuilder = queryContext.searchSourceBuilder(messageList)
                .size(messageList.limit())
                .from(messageList.offset());

        applyHighlightingIfActivated(searchSourceBuilder, job, query);

        final List<Sort> sorts = firstNonNull(messageList.sort(), Collections.singletonList(Sort.create(Message.FIELD_TIMESTAMP, SortOrder.DESC)));
        sorts.forEach(sort -> searchSourceBuilder.sort(sort.field(), sort.order()));
    }

    private void applyHighlightingIfActivated(SearchSourceBuilder searchSourceBuilder, SearchJob job, Query query) {
        if (!configuration.isAllowHighlighting())
            return;

        final QueryStringQueryBuilder highlightQuery = decoratedHighlightQuery(job, query);

        searchSourceBuilder.highlighter(new HighlightBuilder().requireFieldMatch(false)
                .highlightQuery(highlightQuery)
                .field("*")
                .fragmentSize(0)
                .numOfFragments(0));
    }

    private QueryStringQueryBuilder decoratedHighlightQuery(SearchJob job, Query query) {
        final String raw = ((ElasticsearchQueryString) query.query()).queryString();

        final String decorated = this.esQueryDecorators.decorate(raw, job, query, Collections.emptySet());

        return QueryBuilders.queryStringQuery(decorated);
    }

    @Override
    public SearchType.Result doExtractResult(SearchJob job, Query query, MessageList searchType, SearchResult result, MetricAggregation aggregations, ESGeneratedQueryContext queryContext) {
        //noinspection unchecked
        final List<ResultMessageSummary> messages = result.getHits(Map.class, false).stream()
                .map(hit -> ResultMessage.parseFromSource(hit.id, hit.index, (Map<String, Object>) hit.source, hit.highlight))
                .map((resultMessage) -> ResultMessageSummary.create(resultMessage.highlightRanges, resultMessage.getMessage().getFields(), resultMessage.getIndex()))
                .collect(Collectors.toList());

        final String undecoratedQueryString = ((ElasticsearchQueryString)query.query()).queryString();
        final String queryString = this.esQueryDecorators.decorate(undecoratedQueryString, job, query, Collections.emptySet());

        final DateTime from = query.effectiveTimeRange(searchType).getFrom();
        final DateTime to = query.effectiveTimeRange(searchType).getTo();

        final SearchResponse searchResponse = SearchResponse.create(
                undecoratedQueryString,
                queryString,
                Collections.emptySet(),
                messages,
                Collections.emptySet(),
                0,
                result.getTotal(),
                from,
                to
        );

        final SearchResponse decoratedSearchResponse = decorateSearchResponse(searchResponse, searchType.decorators());

        final MessageList.Result.Builder resultBuilder = MessageList.Result.result(searchType.id())
                .messages(decoratedSearchResponse.messages())
                .totalResults(decoratedSearchResponse.totalResults());
        return searchType.name().map(resultBuilder::name).orElse(resultBuilder).build();
    }

    private SearchResponse decorateSearchResponse(SearchResponse searchResponse, List<Decorator> decorators) {
        if (decorators.isEmpty()) {
            return searchResponse;
        }
        final List<SearchResponseDecorator> searchResponseDecorators = decorators
                .stream()
                .sorted(Comparator.comparing(Decorator::order))
                .map(decorator -> this.searchResponseDecorators.get(decorator.type()).create(decorator))
                .collect(Collectors.toList());
        return decoratorProcessor.decorate(searchResponse, searchResponseDecorators);
    }
}

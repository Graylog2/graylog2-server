package org.graylog.plugins.enterprise.search.elasticsearch.searchtypes;

import io.searchbox.core.SearchResult;
import io.searchbox.core.search.aggregation.MetricAggregation;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.graylog.plugins.enterprise.search.Query;
import org.graylog.plugins.enterprise.search.SearchJob;
import org.graylog.plugins.enterprise.search.SearchType;
import org.graylog.plugins.enterprise.search.elasticsearch.ESGeneratedQueryContext;
import org.graylog.plugins.enterprise.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.enterprise.search.searchtypes.MessageList;
import org.graylog.plugins.enterprise.search.searchtypes.Sort;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.plugin.Message;
import org.graylog2.rest.models.messages.responses.ResultMessageSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ESMessageList implements ESSearchTypeHandler<MessageList> {
    private static final Logger LOG = LoggerFactory.getLogger(ESMessageList.class);

    @Override
    public void doGenerateQueryPart(SearchJob job, Query query, MessageList messageList, ESGeneratedQueryContext queryContext) {
        final SearchSourceBuilder searchSourceBuilder = queryContext.searchSourceBuilder(messageList);
        searchSourceBuilder
                .size(messageList.limit() - messageList.offset())
                .from(messageList.offset())
                .highlighter(new HighlightBuilder().requireFieldMatch(false)
                        .highlightQuery(QueryBuilders.queryStringQuery(((ElasticsearchQueryString)query.query()).queryString()))
                        .field("*")
                        .fragmentSize(0)
                        .numOfFragments(0));
        final List<Sort> sorts = messageList.sort();
        if (sorts == null) {
            searchSourceBuilder.sort(Message.FIELD_TIMESTAMP, SortOrder.DESC);
        } else {
            sorts.forEach(sort -> {
                searchSourceBuilder.sort(sort.field(), sort.order());
            });
        }
    }

    @Override
    public SearchType.Result doExtractResult(SearchJob job, Query query, MessageList searchType, SearchResult result, MetricAggregation aggregations, ESGeneratedQueryContext queryContext) {
        //noinspection unchecked
        final List<ResultMessageSummary> messages = result.getHits(Map.class, false).stream()
                .map(hit -> ResultMessage.parseFromSource(hit.id, hit.index, (Map<String, Object>) hit.source, hit.highlight))
                .map((resultMessage) -> ResultMessageSummary.create(resultMessage.highlightRanges, resultMessage.getMessage().getFields(), resultMessage.getIndex()))
                .collect(Collectors.toList());

        return MessageList.Result.result(searchType.id())
                .messages(messages)
                .totalResults(result.getTotal())
                .build();
    }
}

package org.graylog.plugins.enterprise.search.elasticsearch.searchtypes;

import io.searchbox.core.SearchResult;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.graylog.plugins.enterprise.search.QueryJob;
import org.graylog.plugins.enterprise.search.SearchType;
import org.graylog.plugins.enterprise.search.searchtypes.MessageList;
import org.graylog.plugins.enterprise.search.searchtypes.Sort;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.plugin.Message;
import org.graylog2.rest.models.messages.responses.ResultMessageSummary;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ESMessageList implements ESSearchTypeHandler<MessageList> {
    @Override
    public void doGenerateQueryPart(MessageList messageList, SearchSourceBuilder searchSourceBuilder) {
        searchSourceBuilder
                .size(messageList.limit() - messageList.offset())
                .from(messageList.offset());
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
    public SearchType.Result doExtractResult(QueryJob job, MessageList searchType, SearchResult result) {
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

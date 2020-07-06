package org.graylog.storage.elasticsearch7;

import com.google.auto.value.AutoValue;
import com.google.common.collect.Streams;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.apache.shiro.crypto.hash.Md5Hash;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.ClearScrollRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchScrollRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.common.unit.TimeValue;
import org.graylog2.indexer.results.IndexQueryResult;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.indexer.results.ScrollResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

public class ScrollResultES7 extends IndexQueryResult implements ScrollResult {
    private static final Logger LOG = LoggerFactory.getLogger(ScrollResult.class);
    private static final TimeValue DEFAULT_SCROLL = TimeValue.timeValueMinutes(1);

    private final ElasticsearchClient client;
    private final long totalHits;
    private final String scroll;
    private final List<String> fields;
    private final String queryHash;
    private String scrollId;
    private SearchResponse initialResult;
    private int chunkId = 0;

    public interface Factory {
        ScrollResultES7 create(@Assisted SearchResponse initialResult, @Assisted("query") String query, @Assisted("scroll") String scroll, @Assisted List<String> fields);
    }

    @AssistedInject
    public ScrollResultES7(ElasticsearchClient client,
                           @Assisted SearchResponse initialResult,
                           @Assisted("query") String query,
                           @Assisted("scroll") String scroll,
                           @Assisted List<String> fields) {
        super(query, null, initialResult.getTook().getMillis());

        this.client = client;
        this.totalHits = initialResult.getHits().getTotalHits().value;
        checkArgument(initialResult.getScrollId() != null, "Unable to extract scroll id from supplied search result!");
        this.scrollId = initialResult.getScrollId();
        this.initialResult = initialResult;
        this.scroll = scroll;
        this.fields = fields;

        final Md5Hash md5Hash = new Md5Hash(getOriginalQuery());
        queryHash = md5Hash.toHex();

        LOG.debug("[{}] Starting scroll request for query {}", queryHash, getOriginalQuery());
    }

    @Override
    public ScrollChunk nextChunk() throws IOException {
        final SearchResponse result = this.initialResult != null ? this.initialResult : nextSearchResult();
        this.initialResult = null;

        final List<ResultMessage> resultMessages = Streams.stream(result.getHits())
                .map(hit -> ResultMessage.parseFromSource(hit.getId(), hit.getIndex(), hit.getSourceAsMap()))
                .collect(Collectors.toList());

        if (resultMessages.size() == 0) {
            // scroll exhausted
            LOG.debug("[{}] Reached end of scroll results for query {}", queryHash, getOriginalQuery());
            return null;
        }

        this.scrollId = result.getScrollId();

        return ScrollChunkES7.create(fields, chunkId++, resultMessages);
    }

    private SearchResponse nextSearchResult() throws IOException {
        final SearchScrollRequest scrollRequest = new SearchScrollRequest(initialResult.getScrollId());
        scrollRequest.scroll(TimeValue.parseTimeValue(this.scroll, DEFAULT_SCROLL, "scroll time"));
        return client.executeUnsafe((c, requestOptions) -> c.scroll(scrollRequest, requestOptions),
                "Unable to retrieve next chunk from search: ");
    }

    @Override
    public String getQueryHash() {
        return this.queryHash;
    }

    @Override
    public long totalHits() {
        return this.totalHits;
    }

    @Override
    public void cancel() throws IOException {
        final ClearScrollRequest request = new ClearScrollRequest();
        request.addScrollId(scrollId);

        client.executeUnsafe((c, requestOptions) -> c.clearScroll(request, requestOptions),
                "Unable to cancel scrolling search request");
    }

    @AutoValue
    abstract static class ScrollChunkES7 implements ScrollResult.ScrollChunk {
        @Override
        public abstract List<String> getFields();

        @Override
        public abstract int getChunkNumber();

        @Override
        public abstract List<ResultMessage> getMessages();

        static ScrollChunk create(List<String> fields, int chunkNumber, List<ResultMessage> messages) {
            return new AutoValue_ScrollResultES7_ScrollChunkES7(fields, chunkNumber, messages);
        }
    }
}

package org.graylog2.indexer.results;

import io.searchbox.core.SearchResult;

import java.io.IOException;
import java.util.List;

public interface ScrollResult {
    ScrollChunk nextChunk() throws IOException;

    String getQueryHash();

    long totalHits();

    void cancel() throws IOException;

    long tookMs();

    interface Factory {
        ScrollResult create(io.searchbox.core.SearchResult initialResult, String query, List<String> fields);
        ScrollResult create(SearchResult initialResult, String query, String scroll, List<String> fields);
    }

    interface ScrollChunk {
        List<String> getFields();

        int getChunkNumber();

        boolean isFirstChunk();

        List<ResultMessage> getMessages();
    }
}

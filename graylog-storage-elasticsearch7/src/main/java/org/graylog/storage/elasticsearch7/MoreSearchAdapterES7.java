package org.graylog.storage.elasticsearch7;

import org.graylog.events.processor.EventProcessorException;
import org.graylog.events.search.MoreSearch;
import org.graylog.events.search.MoreSearchAdapter;
import org.graylog2.indexer.searches.Sorting;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import java.util.Set;

public class MoreSearchAdapterES7 implements MoreSearchAdapter {
    @Override
    public MoreSearch.Result eventSearch(String queryString, TimeRange timerange, Set<String> affectedIndices, Sorting sorting, int page, int perPage, Set<String> eventStreams, String filterString, Set<String> forbiddenSourceStreams) {
        return null;
    }

    @Override
    public void scrollEvents(String queryString, TimeRange timeRange, Set<String> affectedIndices, Set<String> streams, String scrollTime, int batchSize, ScrollEventsCallback resultCallback) throws EventProcessorException {

    }
}

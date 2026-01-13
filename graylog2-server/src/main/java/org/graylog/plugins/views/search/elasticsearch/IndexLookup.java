package org.graylog.plugins.views.search.elasticsearch;

import org.graylog2.indexer.ranges.IndexRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import java.util.Collection;
import java.util.Set;

public interface IndexLookup {
    Set<String> indexNamesForStreamsInTimeRange(Collection<String> streamIds,
                                                TimeRange timeRange);

    Set<IndexRange> indexRangesForStreamsInTimeRange(Collection<String> streamIds,
                                                     TimeRange timeRange);
}

package org.graylog2.indexer.searches;

import org.graylog2.indexer.ranges.IndexRange;
import org.graylog2.indexer.results.*;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import java.util.List;
import java.util.Set;

public interface SearchesDriver {
    CountResult count(Set<String> affectedIndices, String query, TimeRange range, String filter);

    ScrollResult scroll(Set<String> affectedIndices, Set<String> indexWildcards, Sorting sorting, String filter, String query, TimeRange range, int limit, int offset, List<String> fields);

    SearchResult search(Set<String> indices, Set<IndexRange> indexRanges, SearchesConfig config);

    TermsResult terms(String query, String filter, TimeRange range, Set<String> affectedIndices, String field, List<String> stackedFields, int size, Sorting.Direction sorting);

    TermsHistogramResult termsHistogram(String query, String filter, TimeRange range, Set<String> affectedIndices, String field, List<String> stackedFields, int size, Sorting.Direction sorting, Searches.DateHistogramInterval interval);

    TermsStatsResult termsStats(String query, String filter, TimeRange range, Set<String> affectedIndices, String keyField, String valueField, Searches.TermsStatsOrder order, int size);

    FieldStatsResult fieldStats(String query, String filter, TimeRange range, Set<String> indices, String field, boolean includeCardinality, boolean includeStats, boolean includeCount);

    HistogramResult histogram(String query, String filter, TimeRange range, Set<String> affectedIndices, Searches.DateHistogramInterval interval);

    HistogramResult fieldHistogram(String query, String filter, TimeRange range, Set<String> affectedIndices, String field, Searches.DateHistogramInterval interval, boolean includeStats, boolean includeCardinality);
}

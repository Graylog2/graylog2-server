package org.graylog2.indexer.searches;

import org.graylog2.indexer.searches.timeranges.TimeRange;

import java.util.List;

/**
 * @author Bernd Ahlers <bernd@torch.sh>
 */
public class SearchesConfig {
    private final String query;
    private final String filter;
    private final List<String> fields;
    private final TimeRange range;
    private final int limit;
    private final int offset;
    private final Sorting sorting;

    public SearchesConfig(String query, String filter, List<String> fields, TimeRange range, int limit, int offset, Sorting sorting) {
        this.query = query;
        this.filter = filter;
        this.fields = fields;
        this.range = range;
        this.limit = limit;
        this.offset = offset;
        this.sorting = sorting;
    }

    public String query() {
        return query;
    }

    public String filter() {
        return filter;
    }

    public List<String> fields() {
        return fields;
    }

    public TimeRange range() {
        return range;
    }

    public int limit() {
        return limit;
    }

    public int offset() {
        return offset;
    }

    public Sorting sorting() {
        return sorting;
    }
}

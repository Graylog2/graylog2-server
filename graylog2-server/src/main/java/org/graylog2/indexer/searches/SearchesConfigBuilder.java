package org.graylog2.indexer.searches;

import org.graylog2.indexer.searches.timeranges.TimeRange;

import java.util.List;

public class SearchesConfigBuilder {
    private final static int LIMIT = 150;

    private String query;
    private String filter;
    private List<String> fields;
    private TimeRange range;
    private int limit;
    private int offset;
    private Sorting sorting;

    public static SearchesConfigBuilder newConfig() {
        return new SearchesConfigBuilder();
    }

    public SearchesConfigBuilder setQuery(String query) {
        this.query = query;
        return this;
    }

    public SearchesConfigBuilder setFilter(String filter) {
        this.filter = filter;
        return this;
    }

    public SearchesConfigBuilder setFields(List<String> fields) {
        this.fields = fields;
        return this;
    }

    public SearchesConfigBuilder setRange(TimeRange range) {
        this.range = range;
        return this;
    }

    public SearchesConfigBuilder setLimit(int limit) {
        if (limit <= 0) {
            limit = LIMIT;
        }
        this.limit = limit;
        return this;
    }

    public SearchesConfigBuilder setOffset(int offset) {
        this.offset = offset;
        return this;
    }

    public SearchesConfigBuilder setSorting(Sorting sorting) {
        this.sorting = sorting;
        return this;
    }

    public SearchesConfig build() {
        return new SearchesConfig(query, filter, fields, range, limit, offset, sorting);
    }
}
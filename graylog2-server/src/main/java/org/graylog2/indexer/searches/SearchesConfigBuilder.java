/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
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
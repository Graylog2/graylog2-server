/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.views.search.elasticsearch;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.graylog.plugins.views.search.Filter;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.QueryResult;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.engine.GeneratedQueryContext;
import org.graylog.plugins.views.search.errors.SearchError;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.plugins.views.search.util.UniqueNamer;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ESGeneratedQueryContext implements GeneratedQueryContext {

    private final ElasticsearchBackend elasticsearchBackend;
    private final Map<String, SearchSourceBuilder> searchTypeQueries;
    private Map<Object, Object> contextMap;
    private final UniqueNamer uniqueNamer = new UniqueNamer("agg-");
    private Set<SearchError> errors;
    private final SearchSourceBuilder ssb;
    private final SearchJob job;
    private final Query query;
    private final Set<QueryResult> results;

    private final Map<String, Set<String>> fieldTypes;

    public ESGeneratedQueryContext(ElasticsearchBackend elasticsearchBackend, SearchSourceBuilder ssb, SearchJob job, Query query, Set<QueryResult> results) {
        this.elasticsearchBackend = elasticsearchBackend;
        this.ssb = ssb;
        this.job = job;
        this.query = query;
        this.results = results;
        this.contextMap = Maps.newHashMap();
        this.searchTypeQueries = Maps.newHashMap();
        this.errors = Sets.newHashSet();
        this.fieldTypes = null;
    }

    private ESGeneratedQueryContext(ElasticsearchBackend elasticsearchBackend,
                                    SearchSourceBuilder ssb,
                                    SearchJob job,
                                    Query query,
                                    Set<QueryResult> results,
                                    Map<Object, Object> contextMap,
                                    Map<String, SearchSourceBuilder> searchTypeQueries,
                                    Set<SearchError> errors,
                                    Map<String, Set<String>> fieldTypes) {
        this.elasticsearchBackend = elasticsearchBackend;
        this.ssb = ssb;
        this.job = job;
        this.query = query;
        this.results = results;
        this.contextMap = contextMap;
        this.searchTypeQueries = searchTypeQueries;
        this.errors = errors;
        this.fieldTypes = fieldTypes;
    }

    public ESGeneratedQueryContext withFieldTypes(Map<String, Set<String>> fieldTypes) {
        return new ESGeneratedQueryContext(this.elasticsearchBackend,
                this.ssb,
                this.job,
                this.query,
                this.results,
                this.contextMap,
                this.searchTypeQueries,
                this.errors,
                fieldTypes);
    }

    public SearchSourceBuilder searchSourceBuilder(SearchType searchType) {
        return this.searchTypeQueries.computeIfAbsent(searchType.id(), (ignored) -> {
            final QueryBuilder queryBuilder = generateFilterClause(searchType.filter())
                    .map(filterClause -> (QueryBuilder)new BoolQueryBuilder().must(ssb.query()).must(filterClause))
                    .orElse(ssb.query());
            return ssb.copyWithNewSlice(ssb.slice()).query(queryBuilder);
        });
    }

    Map<String, SearchSourceBuilder> searchTypeQueries() {
        return this.searchTypeQueries;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("elasticsearch query", ssb)
                .toString();
    }

    public Map<Object, Object> contextMap() {
        return contextMap;
    }

    public String nextName() {
        return uniqueNamer.nextName();
    }

    private Optional<QueryBuilder> generateFilterClause(Filter filter) {
        return elasticsearchBackend.generateFilterClause(filter, job, query, results);
    }

    public String seriesName(SeriesSpec seriesSpec, Pivot pivot) {
        return pivot.id() + "-series-" + seriesSpec.literal();
    }

    public void addAggregation(AggregationBuilder builder, SearchType searchType) {
        this.searchTypeQueries().get(searchType.id()).aggregation(builder);
    }

    public void addAggregations(Collection<AggregationBuilder> builders, SearchType searchType) {
        builders.forEach(builder -> this.searchTypeQueries().get(searchType.id()).aggregation(builder));
    }

    public Map<String, Set<String>> fieldTypes() {
        return fieldTypes;
    }

    @Override
    public void addError(SearchError error) {
        errors.add(error);
    }

    @Override
    public Collection<SearchError> errors() {
        return errors;
    }
}

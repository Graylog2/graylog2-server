package org.graylog.plugins.enterprise.search.elasticsearch;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.searchbox.core.search.aggregation.Aggregation;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.graylog.plugins.enterprise.search.Filter;
import org.graylog.plugins.enterprise.search.Query;
import org.graylog.plugins.enterprise.search.QueryResult;
import org.graylog.plugins.enterprise.search.SearchJob;
import org.graylog.plugins.enterprise.search.SearchType;
import org.graylog.plugins.enterprise.search.engine.GeneratedQueryContext;
import org.graylog.plugins.enterprise.search.errors.SearchError;
import org.graylog.plugins.enterprise.search.searchtypes.aggregation.AggregationSpec;
import org.graylog.plugins.enterprise.search.util.UniqueNamer;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ESGeneratedQueryContext implements GeneratedQueryContext {

    private final ElasticsearchBackend elasticsearchBackend;
    private final Map<String, SearchSourceBuilder> searchTypeQueries = Maps.newHashMap();
    // do _NOT_ turn this into a regular hashmap!
    private IdentityHashMap<AggregationSpec, Tuple2<String, Class<? extends Aggregation>>> aggResultTypes = Maps.newIdentityHashMap();
    private Map<Object, Object> contextMap = Maps.newHashMap();
    private final UniqueNamer uniqueNamer = new UniqueNamer("agg-");
    private Set<SearchError> errors = Sets.newHashSet();
    private final SearchSourceBuilder ssb;
    private final SearchJob job;
    private final Query query;
    private final Set<QueryResult> results;

    public ESGeneratedQueryContext(ElasticsearchBackend elasticsearchBackend, SearchSourceBuilder ssb, SearchJob job, Query query, Set<QueryResult> results) {
        this.elasticsearchBackend = elasticsearchBackend;
        this.ssb = ssb;
        this.job = job;
        this.query = query;
        this.results = results;
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

    public void recordAggregationType(AggregationSpec aggregationSpec, String name, Class<? extends Aggregation> aggregationClass) {
        final Tuple2<String, Class<? extends Aggregation>> tuple = Tuple.tuple(name, aggregationClass);
        aggResultTypes.put(aggregationSpec, tuple);
    }

    public Tuple2<String, Class<? extends Aggregation>> typeForAggregationSpec(AggregationSpec aggregationSpec) {
        return aggResultTypes.get(aggregationSpec);
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

    public String filterName(SearchType searchType) {
        return "filtered-" + searchType.id();
    }

    public void addAggregation(AggregationBuilder builder, SearchType searchType) {
        this.searchTypeQueries().get(searchType.id()).aggregation(builder);
    }

    public void addAggregations(Collection<AggregationBuilder> builders, SearchType searchType) {
        builders.forEach(builder -> this.searchTypeQueries().get(searchType.id()).aggregation(builder));
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

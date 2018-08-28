package org.graylog.plugins.enterprise.search.elasticsearch;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.searchbox.core.search.aggregation.Aggregation;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.graylog.plugins.enterprise.search.Filter;
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

    public ESGeneratedQueryContext(ElasticsearchBackend elasticsearchBackend, SearchSourceBuilder ssb) {
        this.elasticsearchBackend = elasticsearchBackend;
        this.ssb = ssb;
    }

    public SearchSourceBuilder searchSourceBuilder(String searchTypeId) {
        final SearchSourceBuilder newSearchSourceBuilder = ssb.copyWithNewSlice(ssb.slice());
        this.searchTypeQueries.put(searchTypeId, newSearchSourceBuilder);
        return newSearchSourceBuilder;
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

    public Optional<QueryBuilder> generateFilterClause(Filter filter) {
        return elasticsearchBackend.generateFilterClause(filter);
    }

    public String filterName(SearchType searchType) {
        return "filtered-" + searchType.id();
    }

    public void addFilteredAggregation(AggregationBuilder builder, SearchType searchType) {
        final Optional<QueryBuilder> filterClause = generateFilterClause(searchType.filter());
        if (filterClause.isPresent()) {
            builder = AggregationBuilders.filter(filterName(searchType), filterClause.get())
                    .subAggregation(builder);
        }
        ssb.aggregation(builder);
    }

    public void addFilteredAggregations(Collection<AggregationBuilder> builders, SearchType searchType) {
        final Optional<QueryBuilder> filterClause = generateFilterClause(searchType.filter());
        if (filterClause.isPresent()) {
            final FilterAggregationBuilder filter = AggregationBuilders.filter(filterName(searchType), filterClause.get());
            builders.forEach(filter::subAggregation);
            ssb.aggregation(filter);
        } else {
            builders.forEach(ssb::aggregation);
        }
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

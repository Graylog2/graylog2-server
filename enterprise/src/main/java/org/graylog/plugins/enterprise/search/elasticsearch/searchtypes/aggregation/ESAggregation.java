package org.graylog.plugins.enterprise.search.elasticsearch.searchtypes.aggregation;

import com.google.common.collect.ImmutableList;
import io.searchbox.core.SearchResult;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.graylog.plugins.enterprise.search.Query;
import org.graylog.plugins.enterprise.search.SearchJob;
import org.graylog.plugins.enterprise.search.SearchType;
import org.graylog.plugins.enterprise.search.elasticsearch.ESGeneratedQueryContext;
import org.graylog.plugins.enterprise.search.elasticsearch.searchtypes.ESSearchTypeHandler;
import org.graylog.plugins.enterprise.search.searchtypes.aggregation.Aggregation;
import org.graylog.plugins.enterprise.search.searchtypes.aggregation.AggregationSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.List;
import java.util.Map;

public class ESAggregation implements ESSearchTypeHandler<Aggregation> {
    private static final Logger LOG = LoggerFactory.getLogger(ESAggregation.class);
    private final Map<String, Provider<ESAggregationSpecHandler<? extends AggregationSpec, ? extends io.searchbox.core.search.aggregation.Aggregation>>> aggregationSpecHandler;
    private final UniqueNamer namer;

    @Inject
    public ESAggregation(Map<String, Provider<ESAggregationSpecHandler<? extends AggregationSpec, ? extends io.searchbox.core.search.aggregation.Aggregation>>> aggregationSpecHandler) {
        this.aggregationSpecHandler = aggregationSpecHandler;
        namer = new UniqueNamer();
    }

    @Override
    public void doGenerateQueryPart(SearchJob job, Query query, Aggregation searchType, ESGeneratedQueryContext queryContext) {
        final SearchSourceBuilder searchSourceBuilder = queryContext.searchSourceBuilder();
        // aggregation specs do not necessarily map 1 to 1 onto elasticsearch aggregations, for example field stacking required multiple
        // nested aggregations to produce the necessary data. our result also does not follow the elasticsearch query result directly
        // for the same reason.
        // thus we ask the spec handlers to consume as much, or as little of the tree as they need to and they will call into
        // our generation methods as needed
        // the top level aggregations we'll ask directly to generate themselves
        searchType.aggregations().forEach(aggSpec -> {
            final AggregationBuilder aggregation = handlerForType(aggSpec.type())
                    .createAggregation(namer.nextName(), aggSpec, this, queryContext);
            LOG.debug("Adding top level aggregation {}: {}", aggregation.getName(), aggregation.getType());
            searchSourceBuilder.aggregation(aggregation);
        });
    }

    public ESAggregationSpecHandler<? extends AggregationSpec, ? extends io.searchbox.core.search.aggregation.Aggregation> handlerForType(String specType) {
        final Provider<ESAggregationSpecHandler<? extends AggregationSpec, ? extends io.searchbox.core.search.aggregation.Aggregation>> handler = aggregationSpecHandler.get(specType);
        if (handler == null) {
            throw new IllegalStateException("Missing aggregation handler for type " + specType + ". Cannot generate query, is a plugin missing?");
        }
        return handler.get();
    }

    @Override
    public SearchType.Result doExtractResult(SearchJob job, Query query, Aggregation searchType, SearchResult queryResult, ESGeneratedQueryContext queryContext) {
        final ImmutableList.Builder<Object> metrics = ImmutableList.builder();
        final ImmutableList.Builder<Object> groups = ImmutableList.builder();
        searchType.metrics().forEach(aggSpec -> {
            final ESAggregationSpecHandler<? extends AggregationSpec, ? extends io.searchbox.core.search.aggregation.Aggregation> handler = handlerForType(aggSpec.type());
            final Object result = handler.handleResult(aggSpec, handler.extractAggregationFromResult(aggSpec, queryResult.getAggregations(), queryContext), this, queryContext);
            metrics.add(result);
        });
        searchType.groups().forEach(aggSpec -> {
            final ESAggregationSpecHandler<? extends AggregationSpec, ? extends io.searchbox.core.search.aggregation.Aggregation> handler = handlerForType(aggSpec.type());
            final Object result = handler.handleResult(aggSpec, handler.extractAggregationFromResult(aggSpec, queryResult.getAggregations(), queryContext), this, queryContext);
            groups.add(result);
        });
        return new Aggregation.Result() {
            @Override
            public String id() {
                return searchType.id();
            }

            @Override
            public List<Object> metrics() {
                return metrics.build();
            }

            @Override
            public List<Object> groups() {
                return groups.build();
            }
        };
    }

    public String nextName() {
        return namer.nextName();
    }

    /* not thread safe, just a convenience class */
    private static class UniqueNamer {
        private long i = 0;

        public String nextName() {
            return "agg_" + i++;
        }
    }
}

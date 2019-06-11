package org.graylog.plugins.enterprise.search.elasticsearch.searchtypes;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;
import io.searchbox.core.SearchResult;
import io.searchbox.core.search.aggregation.MetricAggregation;
import io.searchbox.core.search.aggregation.TermsAggregation;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.sort.SortOrder;
import org.graylog.plugins.enterprise.search.Query;
import org.graylog.plugins.enterprise.search.SearchJob;
import org.graylog.plugins.enterprise.search.SearchType;
import org.graylog.plugins.enterprise.search.elasticsearch.ESGeneratedQueryContext;
import org.graylog.plugins.enterprise.search.searchtypes.GroupBy;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ESGroupBy implements ESSearchTypeHandler<GroupBy> {
    // This is the "WORD SEPARATOR MIDDLE DOT" unicode character. It's used to join and split the term values in a
    // stacked group-by query.
    private static final String STACKED_TERMS_AGG_SEPARATOR = "\u2E31";

    AbstractAggregationBuilder createTermsBuilder(String field, List<String> stackedFields, GroupBy groupBy) {
        final int size = Ints.saturatedCast(groupBy.limit());
        final Terms.Order termsOrder = Terms.Order.count(groupBy.order() == SortOrder.ASC);

        if (stackedFields.isEmpty()) {
            // Wrap terms aggregation in a no-op filter to make sure the result structure is correct when not having
            // stacked fields.
            return AggregationBuilders.filter(filterAggName(groupBy), QueryBuilders.matchAllQuery())
                    .subAggregation(AggregationBuilders.terms(termsAggName(groupBy))
                            .field(field)
                            .size(size)
                            .order(termsOrder));
        }

        // If the methods gets stacked fields, we have to use scripting to concatenate the fields.
        // There is currently no other way to do this. (as of ES 5.6)
        final StringBuilder scriptStringBuilder = new StringBuilder();

        // Build a filter for the terms aggregation to make sure we only get terms for messages where all fields
        // exist.
        final BoolQueryBuilder filterQuery = QueryBuilders.boolQuery();

        // Add the main field
        scriptStringBuilder.append("doc['").append(field).append("'].value");
        filterQuery.must(QueryBuilders.existsQuery(field));

        // Add all other fields
        stackedFields.forEach(f -> {
            // There is no way to use some kind of structured value for the stacked fields in the painless script
            // so we have to use a "special" character (that is hopefully not showing up in any value) to join the
            // stacked field values. That allows us to split the result again later to create a field->value mapping.
            scriptStringBuilder.append(" + \"").append(STACKED_TERMS_AGG_SEPARATOR).append("\" + ");
            scriptStringBuilder.append("doc['").append(f).append("'].value");
            filterQuery.must(QueryBuilders.existsQuery(f));
        });

        return AggregationBuilders.filter(filterAggName(groupBy), filterQuery)
                .subAggregation(AggregationBuilders.terms(termsAggName(groupBy))
                        .script(new Script(ScriptType.INLINE, "painless", scriptStringBuilder.toString(), Collections.emptyMap()))
                        .size(size)
                        .order(termsOrder));
    }

    @Override
    public void doGenerateQueryPart(SearchJob job, Query query, GroupBy groupBy, ESGeneratedQueryContext queryContext) {
        final String mainField = groupBy.fields().get(0);
        final List<String> stackedFields = groupBy.fields().subList(1, groupBy.fields().size());

        queryContext.searchSourceBuilder(groupBy).aggregation(createTermsBuilder(mainField, stackedFields, groupBy));
    }

    @Override
    public SearchType.Result doExtractResult(SearchJob job, Query query, GroupBy groupBy, SearchResult queryResult, MetricAggregation aggregations, ESGeneratedQueryContext queryContext) {
        final TermsAggregation termsAggregation = aggregations
                .getFilterAggregation(filterAggName(groupBy))
                .getTermsAggregation(termsAggName(groupBy));

        return extractTermsAggregationResult(groupBy, termsAggregation);
    }

    GroupBy.Result extractTermsAggregationResult(GroupBy groupBy, TermsAggregation termsAggregation) {
        final ImmutableList.Builder<GroupBy.Group> groups = ImmutableList.builder();

        for (final TermsAggregation.Entry entry : termsAggregation.getBuckets()) {
            // Use the "special" character to split up the terms value so we can create a field->value mapping.
            final List<String> valueList = Splitter.on(STACKED_TERMS_AGG_SEPARATOR).splitToList(entry.getKey());

            // For every field in the field list, get the value from the split up terms value list. After this, we
            // have a mapping of field->value for each bucket.
            final ImmutableList.Builder<GroupBy.GroupField> fields = ImmutableList.builder();
            for (int i = 0; i < groupBy.fields().size(); i++) {
                fields.add(GroupBy.GroupField.builder()
                        .field(groupBy.fields().get(i))
                        .value(valueList.get(i))
                        .build());
            }

            groups.add(GroupBy.Group.builder()
                    .count(entry.getCount())
                    .fields(fields.build())
                    .build());
        }

        return GroupBy.Result.builder()
                .id(groupBy.id())
                .groups(groups.build())
                .build();
    }

    String filterAggName(GroupBy groupBy) {
        return String.format(Locale.ENGLISH, "group-by-filter-%s", groupBy.id());
    }

    String termsAggName(GroupBy groupBy) {
        return String.format(Locale.ENGLISH, "group-by-terms-%s", groupBy.id());
    }
}

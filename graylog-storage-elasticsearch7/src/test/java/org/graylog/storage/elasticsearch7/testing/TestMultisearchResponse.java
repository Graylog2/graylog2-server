/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.storage.elasticsearch7.testing;

import com.google.common.io.Resources;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.MultiSearchResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.common.ParseField;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.common.xcontent.ContextParser;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.common.xcontent.LoggingDeprecationHandler;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.common.xcontent.XContentParser;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.common.xcontent.json.JsonXContent;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.Aggregation;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.composite.ParsedComposite;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.filter.ParsedFilter;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.filter.ParsedFilters;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.global.ParsedGlobal;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.histogram.ParsedAutoDateHistogram;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.histogram.ParsedDateHistogram;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.histogram.ParsedHistogram;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.missing.ParsedMissing;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.nested.ParsedReverseNested;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.range.ParsedDateRange;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.range.ParsedRange;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.sampler.ParsedSampler;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.terms.ParsedDoubleTerms;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.ParsedAvg;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.ParsedCardinality;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.ParsedExtendedStats;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.ParsedMax;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.ParsedMedianAbsoluteDeviation;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.ParsedMin;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.ParsedStats;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.ParsedSum;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.ParsedTopHits;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.ParsedValueCount;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.ParsedWeightedAvg;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.pipeline.ParsedBucketMetricValue;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.pipeline.ParsedDerivative;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.pipeline.ParsedExtendedStatsBucket;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.pipeline.ParsedPercentilesBucket;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.pipeline.ParsedSimpleValue;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.pipeline.ParsedStatsBucket;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.suggest.Suggest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.suggest.phrase.PhraseSuggestion;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.suggest.term.TermSuggestion;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TestMultisearchResponse {
    public static MultiSearchResponse fromFixture(String filename) throws IOException {
        return resultFor(resourceFile(filename));
    }

    private static List<NamedXContentRegistry.Entry> getDefaultNamedXContents() {
        Map<String, ContextParser<Object, ? extends Aggregation>> map = new HashMap<>();
        map.put("cardinality", (p, c) -> ParsedCardinality.fromXContent(p, (String)c));
        map.put("percentiles_bucket", (p, c) -> ParsedPercentilesBucket.fromXContent(p, (String)c));
        map.put("median_absolute_deviation", (p, c) -> ParsedMedianAbsoluteDeviation.fromXContent(p, (String)c));
        map.put("min", (p, c) -> ParsedMin.fromXContent(p, (String)c));
        map.put("max", (p, c) -> ParsedMax.fromXContent(p, (String)c));
        map.put("sum", (p, c) -> ParsedSum.fromXContent(p, (String)c));
        map.put("avg", (p, c) -> ParsedAvg.fromXContent(p, (String)c));
        map.put("weighted_avg", (p, c) -> ParsedWeightedAvg.fromXContent(p, (String)c));
        map.put("value_count", (p, c) -> ParsedValueCount.fromXContent(p, (String)c));
        map.put("simple_value", (p, c) -> ParsedSimpleValue.fromXContent(p, (String)c));
        map.put("derivative", (p, c) -> ParsedDerivative.fromXContent(p, (String)c));
        map.put("bucket_metric_value", (p, c) -> ParsedBucketMetricValue.fromXContent(p, (String)c));
        map.put("stats", (p, c) -> ParsedStats.fromXContent(p, (String)c));
        map.put("stats_bucket", (p, c) -> ParsedStatsBucket.fromXContent(p, (String)c));
        map.put("extended_stats", (p, c) -> ParsedExtendedStats.fromXContent(p, (String)c));
        map.put("extended_stats_bucket", (p, c) -> ParsedExtendedStatsBucket.fromXContent(p, (String)c));
        map.put("histogram", (p, c) -> ParsedHistogram.fromXContent(p, (String)c));
        map.put("date_histogram", (p, c) -> ParsedDateHistogram.fromXContent(p, (String)c));
        map.put("auto_date_histogram", (p, c) -> ParsedAutoDateHistogram.fromXContent(p, (String)c));
        map.put("sterms", (p, c) -> ParsedStringTerms.fromXContent(p, (String)c));
        map.put("lterms", (p, c) -> ParsedLongTerms.fromXContent(p, (String)c));
        map.put("dterms", (p, c) -> ParsedDoubleTerms.fromXContent(p, (String)c));
        map.put("missing", (p, c) -> ParsedMissing.fromXContent(p, (String)c));
        map.put("nested", (p, c) -> ParsedNested.fromXContent(p, (String)c));
        map.put("reverse_nested", (p, c) -> ParsedReverseNested.fromXContent(p, (String)c));
        map.put("global", (p, c) -> ParsedGlobal.fromXContent(p, (String)c));
        map.put("filter", (p, c) -> ParsedFilter.fromXContent(p, (String)c));
        map.put("sampler", (p, c) -> ParsedSampler.fromXContent(p, (String)c));
        map.put("range", (p, c) -> ParsedRange.fromXContent(p, (String)c));
        map.put("date_range", (p, c) -> ParsedDateRange.fromXContent(p, (String)c));
        map.put("filters", (p, c) -> ParsedFilters.fromXContent(p, (String)c));
        map.put("top_hits", (p, c) -> ParsedTopHits.fromXContent(p, (String)c));
        map.put("composite", (p, c) -> ParsedComposite.fromXContent(p, (String)c));

        List<NamedXContentRegistry.Entry> entries = map.entrySet().stream().map((entry) -> new NamedXContentRegistry.Entry(Aggregation.class, new ParseField(entry.getKey()), entry.getValue())).collect(Collectors.toList());
        entries.add(new NamedXContentRegistry.Entry(Suggest.Suggestion.class, new ParseField("term"), (parser, context) -> TermSuggestion.fromXContent(parser, (String)context)));
        entries.add(new NamedXContentRegistry.Entry(Suggest.Suggestion.class, new ParseField("phrase"), (parser, context) -> PhraseSuggestion.fromXContent(parser, (String)context)));
        entries.add(new NamedXContentRegistry.Entry(Suggest.Suggestion.class, new ParseField("completion"), (parser, context) -> CompletionSuggestion.fromXContent(parser, (String)context)));
        return entries;
    }

    private static MultiSearchResponse resultFor(InputStream result) throws IOException {
        final NamedXContentRegistry registry = new NamedXContentRegistry(getDefaultNamedXContents());
        final XContentParser parser = JsonXContent.jsonXContent.createParser(registry, LoggingDeprecationHandler.INSTANCE, result);
        return MultiSearchResponse.fromXContext(parser);
    }

    private static InputStream resourceFile(String filename) {
        try {
            @SuppressWarnings("UnstableApiUsage")
            final URL resource = Resources.getResource(filename);
            final Path path = Paths.get(resource.toURI());
            return Files.newInputStream(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}

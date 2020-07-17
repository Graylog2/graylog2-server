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
package org.graylog.storage.elasticsearch7.views;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.MultiSearchRequest;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.MultiSearchResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.common.ParseField;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.common.xcontent.ContextParser;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.common.xcontent.LoggingDeprecationHandler;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.common.xcontent.XContentParser;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.common.xcontent.json.JsonXContent;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.Aggregation;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.adjacency.ParsedAdjacencyMatrix;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.composite.ParsedComposite;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.filter.ParsedFilter;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.filter.ParsedFilters;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.geogrid.ParsedGeoHashGrid;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.geogrid.ParsedGeoTileGrid;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.global.ParsedGlobal;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.histogram.ParsedAutoDateHistogram;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.histogram.ParsedDateHistogram;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.histogram.ParsedHistogram;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.missing.ParsedMissing;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.nested.ParsedReverseNested;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.range.ParsedBinaryRange;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.range.ParsedDateRange;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.range.ParsedGeoDistance;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.range.ParsedRange;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.sampler.ParsedSampler;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.significant.ParsedSignificantLongTerms;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.significant.ParsedSignificantStringTerms;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.terms.ParsedDoubleTerms;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.ParsedAvg;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.ParsedCardinality;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.ParsedExtendedStats;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.ParsedGeoBounds;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.ParsedGeoCentroid;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.ParsedHDRPercentileRanks;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.ParsedHDRPercentiles;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.ParsedMax;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.ParsedMedianAbsoluteDeviation;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.ParsedMin;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.ParsedScriptedMetric;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.ParsedStats;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.ParsedSum;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.ParsedTDigestPercentileRanks;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.ParsedTDigestPercentiles;
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
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

abstract class ElasticsearchBackendTestBase {
    static final ObjectMapperProvider objectMapperProvider = new ObjectMapperProvider();
    static final ObjectMapper objectMapper = objectMapperProvider.get();

    static List<NamedXContentRegistry.Entry> getDefaultNamedXContents() {
        Map<String, ContextParser<Object, ? extends Aggregation>> map = new HashMap();
        map.put("cardinality", (p, c) -> {
            return ParsedCardinality.fromXContent(p, (String)c);
        });
        map.put("hdr_percentiles", (p, c) -> {
            return ParsedHDRPercentiles.fromXContent(p, (String)c);
        });
        map.put("hdr_percentile_ranks", (p, c) -> {
            return ParsedHDRPercentileRanks.fromXContent(p, (String)c);
        });
        map.put("tdigest_percentiles", (p, c) -> {
            return ParsedTDigestPercentiles.fromXContent(p, (String)c);
        });
        map.put("tdigest_percentile_ranks", (p, c) -> {
            return ParsedTDigestPercentileRanks.fromXContent(p, (String)c);
        });
        map.put("percentiles_bucket", (p, c) -> {
            return ParsedPercentilesBucket.fromXContent(p, (String)c);
        });
        map.put("median_absolute_deviation", (p, c) -> {
            return ParsedMedianAbsoluteDeviation.fromXContent(p, (String)c);
        });
        map.put("min", (p, c) -> {
            return ParsedMin.fromXContent(p, (String)c);
        });
        map.put("max", (p, c) -> {
            return ParsedMax.fromXContent(p, (String)c);
        });
        map.put("sum", (p, c) -> {
            return ParsedSum.fromXContent(p, (String)c);
        });
        map.put("avg", (p, c) -> {
            return ParsedAvg.fromXContent(p, (String)c);
        });
        map.put("weighted_avg", (p, c) -> {
            return ParsedWeightedAvg.fromXContent(p, (String)c);
        });
        map.put("value_count", (p, c) -> {
            return ParsedValueCount.fromXContent(p, (String)c);
        });
        map.put("simple_value", (p, c) -> {
            return ParsedSimpleValue.fromXContent(p, (String)c);
        });
        map.put("derivative", (p, c) -> {
            return ParsedDerivative.fromXContent(p, (String)c);
        });
        map.put("bucket_metric_value", (p, c) -> {
            return ParsedBucketMetricValue.fromXContent(p, (String)c);
        });
        map.put("stats", (p, c) -> {
            return ParsedStats.fromXContent(p, (String)c);
        });
        map.put("stats_bucket", (p, c) -> {
            return ParsedStatsBucket.fromXContent(p, (String)c);
        });
        map.put("extended_stats", (p, c) -> {
            return ParsedExtendedStats.fromXContent(p, (String)c);
        });
        map.put("extended_stats_bucket", (p, c) -> {
            return ParsedExtendedStatsBucket.fromXContent(p, (String)c);
        });
        map.put("geo_bounds", (p, c) -> {
            return ParsedGeoBounds.fromXContent(p, (String)c);
        });
        map.put("geo_centroid", (p, c) -> {
            return ParsedGeoCentroid.fromXContent(p, (String)c);
        });
        map.put("histogram", (p, c) -> {
            return ParsedHistogram.fromXContent(p, (String)c);
        });
        map.put("date_histogram", (p, c) -> {
            return ParsedDateHistogram.fromXContent(p, (String)c);
        });
        map.put("auto_date_histogram", (p, c) -> {
            return ParsedAutoDateHistogram.fromXContent(p, (String)c);
        });
        map.put("sterms", (p, c) -> {
            return ParsedStringTerms.fromXContent(p, (String)c);
        });
        map.put("lterms", (p, c) -> {
            return ParsedLongTerms.fromXContent(p, (String)c);
        });
        map.put("dterms", (p, c) -> {
            return ParsedDoubleTerms.fromXContent(p, (String)c);
        });
        map.put("missing", (p, c) -> {
            return ParsedMissing.fromXContent(p, (String)c);
        });
        map.put("nested", (p, c) -> {
            return ParsedNested.fromXContent(p, (String)c);
        });
        map.put("reverse_nested", (p, c) -> {
            return ParsedReverseNested.fromXContent(p, (String)c);
        });
        map.put("global", (p, c) -> {
            return ParsedGlobal.fromXContent(p, (String)c);
        });
        map.put("filter", (p, c) -> {
            return ParsedFilter.fromXContent(p, (String)c);
        });
        map.put("sampler", (p, c) -> {
            return ParsedSampler.fromXContent(p, (String)c);
        });
        map.put("geohash_grid", (p, c) -> {
            return ParsedGeoHashGrid.fromXContent(p, (String)c);
        });
        map.put("geotile_grid", (p, c) -> {
            return ParsedGeoTileGrid.fromXContent(p, (String)c);
        });
        map.put("range", (p, c) -> {
            return ParsedRange.fromXContent(p, (String)c);
        });
        map.put("date_range", (p, c) -> {
            return ParsedDateRange.fromXContent(p, (String)c);
        });
        map.put("geo_distance", (p, c) -> {
            return ParsedGeoDistance.fromXContent(p, (String)c);
        });
        map.put("filters", (p, c) -> {
            return ParsedFilters.fromXContent(p, (String)c);
        });
        map.put("adjacency_matrix", (p, c) -> {
            return ParsedAdjacencyMatrix.fromXContent(p, (String)c);
        });
        map.put("siglterms", (p, c) -> {
            return ParsedSignificantLongTerms.fromXContent(p, (String)c);
        });
        map.put("sigsterms", (p, c) -> {
            return ParsedSignificantStringTerms.fromXContent(p, (String)c);
        });
        map.put("scripted_metric", (p, c) -> {
            return ParsedScriptedMetric.fromXContent(p, (String)c);
        });
        map.put("ip_range", (p, c) -> {
            return ParsedBinaryRange.fromXContent(p, (String)c);
        });
        map.put("top_hits", (p, c) -> {
            return ParsedTopHits.fromXContent(p, (String)c);
        });
        map.put("composite", (p, c) -> {
            return ParsedComposite.fromXContent(p, (String)c);
        });
        List<NamedXContentRegistry.Entry> entries = (List)map.entrySet().stream().map((entry) -> {
            return new NamedXContentRegistry.Entry(Aggregation.class, new ParseField((String)entry.getKey(), new String[0]), (ContextParser)entry.getValue());
        }).collect(Collectors.toList());
        entries.add(new NamedXContentRegistry.Entry(Suggest.Suggestion.class, new ParseField("term", new String[0]), (parser, context) -> {
            return TermSuggestion.fromXContent(parser, (String)context);
        }));
        entries.add(new NamedXContentRegistry.Entry(Suggest.Suggestion.class, new ParseField("phrase", new String[0]), (parser, context) -> {
            return PhraseSuggestion.fromXContent(parser, (String)context);
        }));
        entries.add(new NamedXContentRegistry.Entry(Suggest.Suggestion.class, new ParseField("completion", new String[0]), (parser, context) -> {
            return CompletionSuggestion.fromXContent(parser, (String)context);
        }));
        return entries;
    }
    MultiSearchResponse resultFor(InputStream result) throws IOException {
        final NamedXContentRegistry registry = new NamedXContentRegistry(getDefaultNamedXContents());
        final XContentParser parser = JsonXContent.jsonXContent.createParser(registry, LoggingDeprecationHandler.INSTANCE, result);
        return MultiSearchResponse.fromXContext(parser);
    }

    InputStream resourceFile(String filename) {
        try {
            @SuppressWarnings("UnstableApiUsage")
            final URL resource = Resources.getResource(this.getClass(), filename);
            final Path path = Paths.get(resource.toURI());
            return Files.newInputStream(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    List<String> indicesOf(MultiSearchRequest clientRequest) throws IOException {
        return clientRequest.requests().stream()
                .flatMap(request -> Arrays.stream(request.indices()))
                .distinct()
                .collect(Collectors.toList());
    }
}

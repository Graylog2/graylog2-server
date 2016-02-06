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
package org.graylog2.dashboards.widgets;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.graylog2.indexer.results.FieldStatsResult;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.indexer.searches.timeranges.RelativeRange;
import org.graylog2.indexer.searches.timeranges.TimeRange;
import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static com.google.common.base.Strings.isNullOrEmpty;

public class StatisticalCountWidget extends SearchResultCountWidget {
    private static final Logger log = LoggerFactory.getLogger(StatisticalCountWidget.class);

    public enum StatisticalFunction {
        COUNT("count"),
        MEAN("mean"),
        STANDARD_DEVIATION("std_deviation"),
        MIN("min"),
        MAX("max"),
        SUM("sum"),
        VARIANCE("variance"),
        SUM_OF_SQUARES("squares"),
        CARDINALITY("cardinality");

        private final String function;

        StatisticalFunction(String function) {
            this.function = function;
        }

        @Override
        public String toString() {
            return this.function;
        }

        public static StatisticalFunction fromString(String function) {
            for (StatisticalFunction statisticalFunction : StatisticalFunction.values()) {
                if (statisticalFunction.toString().equals(function)) {
                    return statisticalFunction;
                }
            }

            throw new IllegalArgumentException("Statistic function " + function + " is not supported");
        }
    }

    private final StatisticalFunction statsFunction;
    private final String field;
    private final String streamId;

    public StatisticalCountWidget(MetricRegistry metricRegistry,
                                  Searches searches,
                                  String id,
                                  String description,
                                  WidgetCacheTime cacheTime,
                                  Map<String, Object> config,
                                  String query,
                                  TimeRange timeRange,
                                  String creatorUserId) {
        super(metricRegistry,
              Type.STATS_COUNT,
              searches,
              id,
              description,
              cacheTime,
              config,
              query,
              timeRange,
              creatorUserId);
        this.field = (String) config.get("field");
        String statsFunction = (String) config.get("stats_function");
        // We accidentally modified the standard deviation function name, we need this to make old widgets work again
        this.statsFunction = (statsFunction.equals("stddev")) ? StatisticalFunction.STANDARD_DEVIATION : StatisticalFunction.fromString(statsFunction);
        this.streamId = (String) config.get("stream_id");
    }

    @Override
    public Map<String, Object> getPersistedConfig() {
        final Map<String, Object> inheritedConfig = super.getPersistedConfig();
        final ImmutableMap.Builder<String, Object> persistedConfig = ImmutableMap.builder();
        persistedConfig.putAll(inheritedConfig);
        persistedConfig.put("field", field);
        persistedConfig.put("stats_function", statsFunction.toString());
        if (!isNullOrEmpty(streamId)) {
            persistedConfig.put("stream_id", streamId);
        }

        return persistedConfig.build();
    }

    private Number getStatisticalValue(FieldStatsResult fieldStatsResult) {
        switch (statsFunction) {
            case COUNT:
                return fieldStatsResult.getCount();
            case MEAN:
                return fieldStatsResult.getMean();
            case STANDARD_DEVIATION:
                return fieldStatsResult.getStdDeviation();
            case MIN:
                return fieldStatsResult.getMin();
            case MAX:
                return fieldStatsResult.getMax();
            case SUM:
                return fieldStatsResult.getSum();
            case VARIANCE:
                return fieldStatsResult.getVariance();
            case SUM_OF_SQUARES:
                return fieldStatsResult.getSumOfSquares();
            case CARDINALITY:
                return fieldStatsResult.getCardinality();
            default:
                throw new IllegalArgumentException("Statistic function " + statsFunction + " is not supported");
        }
    }

    @Override
    protected ComputationResult compute() {
        try {
            final String filter;
            if (!isNullOrEmpty(streamId)) {
                filter = "streams:" + streamId;
            } else {
                filter = null;
            }

            final TimeRange timeRange = this.getTimeRange();

            boolean needsCardinality = statsFunction.equals(StatisticalFunction.CARDINALITY);
            boolean needsCount = statsFunction.equals(StatisticalFunction.COUNT);
            final FieldStatsResult fieldStatsResult =
                    getSearches().fieldStats(field,
                                             query,
                                             filter,
                                             timeRange,
                                             needsCardinality,
                                             !(needsCount || needsCardinality),
                                             needsCount);
            if (trend && timeRange instanceof RelativeRange) {
                DateTime toPrevious = timeRange.getFrom();
                DateTime fromPrevious = toPrevious.minus(Seconds.seconds(((RelativeRange) timeRange).getRange()));
                TimeRange previousTimeRange = AbsoluteRange.create(fromPrevious, toPrevious);

                final FieldStatsResult previousFieldStatsResult =
                        getSearches().fieldStats(field,
                                                 query,
                                                 filter,
                                                 previousTimeRange,
                                                 needsCardinality,
                                                 !(needsCount || needsCardinality),
                                                 needsCount);
                Map<String, Object> results = Maps.newHashMap();
                results.put("now", getStatisticalValue(fieldStatsResult));
                results.put("previous", getStatisticalValue(previousFieldStatsResult));
                long tookMs = fieldStatsResult.took().millis() + previousFieldStatsResult.took().millis();

                return new ComputationResult(results, tookMs);
            } else {
                return new ComputationResult(getStatisticalValue(fieldStatsResult), fieldStatsResult.took().millis());
            }
        } catch (Searches.FieldTypeException e) {
            log.warn("Invalid field provided, returning 'NaN'", e);
            return new ComputationResult(Double.NaN, 0);
        }
    }
}

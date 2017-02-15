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
package org.graylog2.dashboards.widgets.strategies;

import com.google.common.collect.Maps;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.indexer.results.FieldStatsResult;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.plugin.dashboards.widgets.ComputationResult;
import org.graylog2.plugin.dashboards.widgets.WidgetStrategy;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static com.google.common.base.Strings.isNullOrEmpty;

public class StatisticalCountWidgetStrategy extends SearchResultCountWidgetStrategy {
    public interface Factory extends WidgetStrategy.Factory<StatisticalCountWidgetStrategy> {
        @Override
        StatisticalCountWidgetStrategy create(Map<String, Object> config, TimeRange timeRange, String widgetId);
    }

    private static final Logger log = LoggerFactory.getLogger(StatisticalCountWidgetStrategy.class);

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

    @AssistedInject
    public StatisticalCountWidgetStrategy(Searches searches,
                                          @Assisted Map<String, Object> config,
                                          @Assisted TimeRange timeRange,
                                          @Assisted String widgetId) {
        super(searches, config, timeRange, widgetId);
        this.field = (String) config.get("field");
        String statsFunction = (String) config.get("stats_function");
        // We accidentally modified the standard deviation function name, we need this to make old widgets work again
        this.statsFunction = ("stddev".equals(statsFunction)) ? StatisticalFunction.STANDARD_DEVIATION : StatisticalFunction.fromString(statsFunction);
        this.streamId = (String) config.get("stream_id");
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
    public ComputationResult compute() {
        try {
            final String filter;
            if (!isNullOrEmpty(streamId)) {
                filter = "streams:" + streamId;
            } else {
                filter = null;
            }

            final TimeRange timeRange = this.timeRange;

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

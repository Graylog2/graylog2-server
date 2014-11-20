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
package org.graylog2.dashboards.widgets;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.graylog2.indexer.IndexHelper;
import org.graylog2.indexer.results.FieldStatsResult;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.indexer.searches.timeranges.RelativeRange;
import org.graylog2.indexer.searches.timeranges.TimeRange;
import org.joda.time.DateTime;
import org.joda.time.Seconds;

import java.util.Map;

import static com.google.common.base.Strings.isNullOrEmpty;

public class StatisticalCountWidget extends SearchResultCountWidget {
    private final String statsFunction;
    private final String field;
    private final String streamId;

    public StatisticalCountWidget(MetricRegistry metricRegistry, Searches searches, String id, String description, int cacheTime, Map<String, Object> config, String query, TimeRange timeRange, String creatorUserId) {
        super(metricRegistry, Type.STATS_COUNT, searches, id, description, cacheTime, config, query, timeRange, creatorUserId);
        this.field = (String) config.get("field");
        this.statsFunction = (String) config.get("stats_function");
        this.streamId = (String) config.get("stream_id");
    }

    @Override
    public Map<String, Object> getPersistedConfig() {
        final Map<String, Object> inheritedConfig = super.getPersistedConfig();
        final ImmutableMap.Builder<String, Object> persistedConfig = ImmutableMap.builder();
        persistedConfig.putAll(inheritedConfig);
        persistedConfig.put("field", field);
        persistedConfig.put("stats_function", statsFunction);
        if (!isNullOrEmpty(streamId)) {
            persistedConfig.put("stream_id", streamId);
        }

        return persistedConfig.build();
    }

    private Number getStatisticalValue(FieldStatsResult fieldStatsResult) {
        final Number statisticalValue;

        switch (statsFunction) {
            case "count":
                statisticalValue = fieldStatsResult.getCount();
                break;
            case "mean":
                statisticalValue = fieldStatsResult.getMean();
                break;
            case "stddev":
                statisticalValue = fieldStatsResult.getStdDeviation();
                break;
            case "min":
                statisticalValue = fieldStatsResult.getMin();
                break;
            case "max":
                statisticalValue = fieldStatsResult.getMax();
                break;
            case "sum":
                statisticalValue = fieldStatsResult.getSum();
                break;
            case "variance":
                statisticalValue = fieldStatsResult.getVariance();
                break;
            case "squares":
                statisticalValue = fieldStatsResult.getSumOfSquares();
                break;
            default:
                throw new IllegalArgumentException("Statistic function " + statsFunction + " is not supported");
        }

        return statisticalValue;
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
            FieldStatsResult fieldStatsResult = getSearches().fieldStats(field, query, filter, timeRange);
            if (trend && timeRange instanceof RelativeRange) {
                DateTime toPrevious = timeRange.getFrom();
                DateTime fromPrevious = toPrevious.minus(Seconds.seconds(((RelativeRange) timeRange).getRange()));
                TimeRange previousTimeRange = new AbsoluteRange(fromPrevious, toPrevious);
                FieldStatsResult previousFieldStatsResult = getSearches().fieldStats(field, query, filter, previousTimeRange);

                Map<String, Object> results = Maps.newHashMap();
                results.put("now", getStatisticalValue(fieldStatsResult));
                results.put("previous", getStatisticalValue(previousFieldStatsResult));
                long tookMs = fieldStatsResult.took().millis() + previousFieldStatsResult.took().millis();

                return new ComputationResult(results, tookMs);
            } else {
                return new ComputationResult(getStatisticalValue(fieldStatsResult), fieldStatsResult.took().millis());
            }
        } catch (IndexHelper.InvalidRangeFormatException e) {
            throw new RuntimeException("Invalid timerange format.", e);
        } catch (Searches.FieldTypeException e) {
            throw new RuntimeException("Invalid field provided.", e);
        }
    }
}

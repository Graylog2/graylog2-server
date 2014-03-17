/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
 *
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
 *
 */
package org.graylog2.alerts.types;

import org.elasticsearch.search.SearchHits;
import org.graylog2.Core;
import org.graylog2.alerts.AlertCondition;
import org.graylog2.indexer.IndexHelper;
import org.graylog2.indexer.results.FieldStatsResult;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.streams.Stream;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class FieldValueAlertCondition extends AlertCondition {

    private static final Logger LOG = LoggerFactory.getLogger(FieldValueAlertCondition.class);
    private List<ResultMessage> searchHits = null;

    public enum CheckType {
        MEAN, MIN, MAX, SUM, STDDEV
    }

    public enum ThresholdType {
        LOWER, HIGHER
    }

    private final int grace;
    private final int time;
    private final ThresholdType thresholdType;
    private final Number threshold;
    private final CheckType type;
    private final String field;

    public FieldValueAlertCondition(Core core, Stream stream, String id, DateTime createdAt, String creatorUserId, Map<String, Object> parameters) {
        super(core, stream, id, Type.FIELD_VALUE, createdAt, creatorUserId, parameters);

        this.grace = (Integer) parameters.get("grace");
        this.time = (Integer) parameters.get("time");
        this.thresholdType = ThresholdType.valueOf(((String) parameters.get("threshold_type")).toUpperCase());
        this.threshold = (Number) parameters.get("threshold");
        this.type = CheckType.valueOf(((String) parameters.get("type")).toUpperCase());
        this.field = (String) parameters.get("field");
    }

    @Override
    public String getDescription() {
        return new StringBuilder()
                .append("time: ").append(time)
                .append(", field: ").append(field)
                .append(", check type: ").append(type.toString().toLowerCase())
                .append(", threshold_type: ").append(thresholdType.toString().toLowerCase())
                .append(", threshold: ").append(threshold)
                .append(", grace: ").append(grace)
                .toString();
    }

    @Override
    protected CheckResult runCheck() {
        this.searchHits = null;
        try {
            String filter = "streams:"+stream.getId();
            FieldStatsResult fieldStatsResult = core.getIndexer().searches().fieldStats(field, "*", filter, new RelativeRange(time * 60));
            if (getBacklog() != null && getBacklog() > 0)
                this.searchHits = fieldStatsResult.getSearchHits();

            if (fieldStatsResult.getCount() == 0) {
                LOG.debug("Alert check <{}> did not match any messages. Returning not triggered.", type);
                return new CheckResult(false);
            }

            double result;
            switch (type) {
                case MEAN:
                    result = fieldStatsResult.getMean();
                    break;
                case MIN:
                    result = fieldStatsResult.getMin();
                    break;
                case MAX:
                    result = fieldStatsResult.getMax();
                    break;
                case SUM:
                    result = fieldStatsResult.getSum();
                    break;
                case STDDEV:
                    result = fieldStatsResult.getStdDeviation();
                    break;
                default:
                    LOG.error("No such field value check type: [{}]. Returning not triggered.", type);
                    return new CheckResult(false);
            }

            LOG.debug("Alert check <{}> result: [{}]", id, result);

            if(Double.isInfinite(result)) {
                // This happens when there are no ES results/docs.
                LOG.debug("Infinite value. Returning not triggered.");
                return new CheckResult(false);
            }

            boolean triggered = false;
            switch (thresholdType) {
                case HIGHER:
                    triggered = result > threshold.doubleValue();
                    break;
                case LOWER:
                    triggered = result < threshold.doubleValue();
                    break;
            }

            if (triggered) {
                StringBuilder resultDescription = new StringBuilder();

                resultDescription.append("Field ").append(field).append(" had a ")
                        .append(type.toString().toLowerCase()).append(" of ")
                        .append(result).append(" in the last ")
                        .append(time).append(" minutes with trigger condition ")
                        .append(thresholdType.toString().toLowerCase()).append(" than ")
                        .append(threshold).append(". ")
                        .append("(Current grace time: ").append(grace).append(" minutes)");

                return new CheckResult(true, this, resultDescription.toString(), Tools.iso8601());
            } else {
                return new CheckResult(false);
            }
        } catch (InvalidRangeParametersException e) {
            // cannot happen lol
            LOG.error("Invalid timerange.", e);
            return null;
        } catch (IndexHelper.InvalidRangeFormatException e) {
            // lol same here
            LOG.error("Invalid timerange format.", e);
            return null;
        } catch (Searches.FieldTypeException e) {
            LOG.debug("Field [{}] seems not to have a numerical type or doesn't even exist at all. Returning not triggered.", field, e);
            return new CheckResult(false);
        }
    }

    @Override
    public List<ResultMessage> getSearchHits() {
        return this.searchHits;
    }
}

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
package org.graylog2.alerts.types;

import com.google.common.collect.Lists;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.alerts.AbstractAlertCondition;
import org.graylog2.indexer.InvalidRangeFormatException;
import org.graylog2.indexer.results.FieldStatsResult;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageSummary;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.streams.Stream;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;

public class FieldValueAlertCondition extends AbstractAlertCondition {
    private static final Logger LOG = LoggerFactory.getLogger(FieldValueAlertCondition.class);

    public enum CheckType {
        MEAN, MIN, MAX, SUM, STDDEV
    }

    public enum ThresholdType {
        LOWER, HIGHER
    }

    public interface Factory {
        FieldValueAlertCondition createAlertCondition(Stream stream, String id, DateTime createdAt, @Assisted("userid") String creatorUserId, Map<String, Object> parameters);
    }

    private final int time;
    private final ThresholdType thresholdType;
    private final Number threshold;
    private final CheckType type;
    private final String field;
    private final DecimalFormat decimalFormat;
    private final Searches searches;
    private List<Message> searchHits = Lists.newArrayList();

    @AssistedInject
    public FieldValueAlertCondition(Searches searches, @Assisted Stream stream, @Nullable @Assisted String id, @Assisted DateTime createdAt, @Assisted("userid") String creatorUserId, @Assisted Map<String, Object> parameters) {
        super(stream, id, Type.FIELD_VALUE, createdAt, creatorUserId, parameters);
        this.searches = searches;

        this.decimalFormat = new DecimalFormat("#.###", DecimalFormatSymbols.getInstance(Locale.ENGLISH));

        this.time = (Integer) parameters.get("time");
        this.thresholdType = ThresholdType.valueOf(((String) parameters.get("threshold_type")).toUpperCase(Locale.ENGLISH));
        this.threshold = (Number) parameters.get("threshold");
        this.type = CheckType.valueOf(((String) parameters.get("type")).toUpperCase(Locale.ENGLISH));
        this.field = (String) parameters.get("field");

        checkArgument(!isNullOrEmpty(field), "\"field\" must not be empty.");
    }

    @Override
    public String getDescription() {
        return "time: " + time
                + ", field: " + field
                + ", check type: " + type.toString().toLowerCase(Locale.ENGLISH)
                + ", threshold_type: " + thresholdType.toString().toLowerCase(Locale.ENGLISH)
                + ", threshold: " + decimalFormat.format(threshold)
                + ", grace: " + grace;
    }


    @Override
    protected CheckResult runCheck() {
        try {
            final String filter = "streams:" + stream.getId();
            // TODO we don't support cardinality yet
            final FieldStatsResult fieldStatsResult = searches.fieldStats(field, "*", filter, new RelativeRange(time * 60), false, true, false);

            if (fieldStatsResult.getCount() == 0) {
                LOG.debug("Alert check <{}> did not match any messages. Returning not triggered.", type);
                return new NegativeCheckResult(this);
            }

            final double result;
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
                    return new NegativeCheckResult(this);
            }

            LOG.debug("Alert check <{}> result: [{}]", id, result);

            if (Double.isInfinite(result)) {
                // This happens when there are no ES results/docs.
                LOG.debug("Infinite value. Returning not triggered.");
                return new NegativeCheckResult(this);
            }

            final boolean triggered;
            switch (thresholdType) {
                case HIGHER:
                    triggered = result > threshold.doubleValue();
                    break;
                case LOWER:
                    triggered = result < threshold.doubleValue();
                    break;
                default:
                    triggered = false;
            }

            if (triggered) {
                final String resultDescription = "Field " + field + " had a " + type + " of "
                        + decimalFormat.format(result) + " in the last " + time + " minutes with trigger condition "
                        + thresholdType + " than " + decimalFormat.format(threshold) + ". "
                        + "(Current grace time: " + grace + " minutes)";

                final List<MessageSummary> summaries;
                if (getBacklog() > 0) {
                    final List<ResultMessage> searchResult = fieldStatsResult.getSearchHits();
                    summaries = Lists.newArrayListWithCapacity(searchResult.size());
                    for (ResultMessage resultMessage : searchResult) {
                        final Message msg = new Message(resultMessage.getMessage());
                        this.searchHits.add(msg);
                        summaries.add(new MessageSummary(resultMessage.getIndex(), msg));
                    }
                } else {
                    summaries = Collections.emptyList();
                }

                return new CheckResult(true, this, resultDescription, Tools.iso8601(), summaries);
            } else {
                return new NegativeCheckResult(this);
            }
        } catch (InvalidRangeParametersException e) {
            // cannot happen lol
            LOG.error("Invalid timerange.", e);
            return null;
        } catch (InvalidRangeFormatException e) {
            // lol same here
            LOG.error("Invalid timerange format.", e);
            return null;
        } catch (Searches.FieldTypeException e) {
            LOG.debug("Field [{}] seems not to have a numerical type or doesn't even exist at all. Returning not triggered.", field, e);
            return new NegativeCheckResult(this);
        }
    }

    @Override
    public List<Message> getSearchHits() {
        return Lists.newArrayList(searchHits);
    }
}

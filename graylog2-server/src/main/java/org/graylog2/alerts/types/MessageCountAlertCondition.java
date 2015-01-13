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
package org.graylog2.alerts.types;

import com.google.common.collect.Lists;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.alerts.AbstractAlertCondition;
import org.graylog2.indexer.InvalidRangeFormatException;
import org.graylog2.indexer.results.CountResult;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.indexer.results.SearchResult;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.indexer.searches.Sorting;
import org.graylog2.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.streams.Stream;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MessageCountAlertCondition extends AbstractAlertCondition {
    private static final Logger LOG = LoggerFactory.getLogger(MessageCountAlertCondition.class);

    public enum ThresholdType {
        MORE, LESS
    }

    public interface Factory {
        MessageCountAlertCondition createAlertCondition(Stream stream, String id, DateTime createdAt, @Assisted("userid") String creatorUserId, Map<String, Object> parameters);
    }

    private final int time;
    private final ThresholdType thresholdType;
    private final int threshold;
    private List<Message> searchHits = Collections.emptyList();
    private final Searches searches;

    @AssistedInject
    public MessageCountAlertCondition(Searches searches, @Assisted Stream stream, @Nullable @Assisted String id, @Assisted DateTime createdAt, @Assisted("userid") String creatorUserId, @Assisted Map<String, Object> parameters) {
        super(stream, id, Type.MESSAGE_COUNT, createdAt, creatorUserId, parameters);

        this.searches = searches;
        this.time = (Integer) parameters.get("time");
        this.thresholdType = ThresholdType.valueOf(((String) parameters.get("threshold_type")).toUpperCase());
        this.threshold = (Integer) parameters.get("threshold");
    }

    @Override
    public String getDescription() {
        return "time: " + time
                + ", threshold_type: " + thresholdType.toString().toLowerCase()
                + ", threshold: " + threshold
                + ", grace: " + grace;
    }

    @Override
    protected CheckResult runCheck() {
        this.searchHits = Collections.emptyList();
        try {
            String filter = "streams:" + stream.getId();
            CountResult result = searches.count("*", new RelativeRange(time * 60), filter);
            long count = result.getCount();

            LOG.debug("Alert check <{}> result: [{}]", id, count);

            boolean triggered = false;
            switch (thresholdType) {
                case MORE:
                    triggered = count > threshold;
                    break;
                case LESS:
                    triggered = count < threshold;
                    break;
            }

            if (triggered) {
                Integer backlogSize = getBacklog();
                if (backlogSize != null && backlogSize > 0) {
                    SearchResult backlogResult = searches.search("*", filter, new RelativeRange(time * 60), backlogSize, 0, new Sorting("timestamp", Sorting.Direction.DESC));
                    this.searchHits = Lists.newArrayList();
                    for (ResultMessage resultMessage : backlogResult.getResults()) {
                        searchHits.add(new Message(resultMessage.getMessage()));
                    }
                }

                final String resultDescription = "Stream had " + count + " messages in the last " + time
                        + " minutes with trigger condition " + thresholdType.toString().toLowerCase()
                        + " than " + threshold + " messages. " + "(Current grace time: " + grace + " minutes)";
                return new CheckResult(true, this, resultDescription, Tools.iso8601());
            } else {
                return new CheckResult(false);
            }
        } catch (InvalidRangeParametersException e) {
            // cannot happen lol
            LOG.error("Invalid timerange.", e);
            return null;
        } catch (InvalidRangeFormatException e) {
            // lol same here
            LOG.error("Invalid timerange format.", e);
            return null;
        }
    }

    @Override
    public List<Message> getSearchHits() {
        return this.searchHits;
    }
}
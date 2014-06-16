/*
 * Copyright 2012-2014 TORCH GmbH
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
 */
package org.graylog2.alerts.types;

import com.google.common.collect.Lists;
import org.graylog2.alerts.AbstractAlertCondition;
import org.graylog2.indexer.IndexHelper;
import org.graylog2.indexer.Indexer;
import org.graylog2.indexer.results.CountResult;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.indexer.results.SearchResult;
import org.graylog2.indexer.searches.Sorting;
import org.graylog2.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.Message;
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
public class MessageCountAlertCondition extends AbstractAlertCondition {

    private static final Logger LOG = LoggerFactory.getLogger(MessageCountAlertCondition.class);

    public enum ThresholdType {
        MORE, LESS
    }

    private final int grace;
    private final int time;
    private final ThresholdType thresholdType;
    private final int threshold;
    private List<Message> searchHits = null;

    public MessageCountAlertCondition(Stream stream, String id, DateTime createdAt, String creatorUserId, Map<String, Object> parameters) {
        super(stream, id, Type.MESSAGE_COUNT, createdAt, creatorUserId, parameters);

        this.grace = (Integer) parameters.get("grace");
        this.time = (Integer) parameters.get("time");
        this.thresholdType = ThresholdType.valueOf(((String) parameters.get("threshold_type")).toUpperCase());
        this.threshold = (Integer) parameters.get("threshold");
    }

    @Override
    public String getDescription() {
        return new StringBuilder()
                .append("time: ").append(time)
                .append(", threshold_type: ").append(thresholdType.toString().toLowerCase())
                .append(", threshold: ").append(threshold)
                .append(", grace: ").append(grace)
                .toString();
    }

    @Override
    protected CheckResult runCheck(Indexer indexer) {
        this.searchHits = null;
        try {
            String filter = "streams:"+stream.getId();
            CountResult result = indexer.searches().count("*", new RelativeRange(time * 60), filter);
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
                    SearchResult backlogResult = indexer.searches().search("*", filter, new RelativeRange(time * 60), backlogSize, 0, new Sorting("timestamp", Sorting.Direction.DESC));
                    this.searchHits = Lists.newArrayList();
                    for (ResultMessage resultMessage : backlogResult.getResults()) {
                        searchHits.add(new Message(resultMessage.getMessage()));
                    }
                }

                StringBuilder resultDescription = new StringBuilder();

                resultDescription.append("Stream had ").append(count).append(" messages in the last ")
                        .append(time).append(" minutes with trigger condition ")
                        .append(thresholdType.toString().toLowerCase()).append(" than ")
                        .append(threshold).append(" messages. ")
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
        }
    }

    @Override
    public List<Message> getSearchHits() {
        return this.searchHits;
    }
}

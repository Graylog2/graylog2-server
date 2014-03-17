/**
 * Copyright 2012 Lennart Koopmann <lennart@socketfeed.com>
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
package org.graylog2.periodical;
import com.beust.jcommander.internal.Lists;
import org.elasticsearch.search.SearchHit;
import org.graylog2.alerts.Alert;
import org.graylog2.alerts.AlertCondition;
import org.graylog2.alerts.AlertSender;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.alarms.transports.TransportConfigurationException;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class AlertScannerThread extends Periodical {

    private static final Logger LOG = LoggerFactory.getLogger(AlertScannerThread.class);
    
    @Override
    public void run() {
        LOG.debug("Running alert checks.");

        List<Stream> alertedStreams = StreamImpl.loadAllWithConfiguredAlertConditions(core);

        LOG.debug("There are {} streams with configured alert conditions.", alertedStreams.size());

        // Load all streams that have configured alert conditions.
        for (Stream streamIF : alertedStreams) {
            StreamImpl stream = (StreamImpl) streamIF;

            LOG.debug("Stream [{}] has [{}] configured alert conditions.", stream, stream.getAlertConditions().size());

            // Check if a threshold is reached.
            for (AlertCondition alertCondition : stream.getAlertConditions()) {
                try {
                    AlertCondition.CheckResult result = alertCondition.triggered();
                    if (result.isTriggered()) {
                        // Alert is triggered!
                        LOG.info("Alert condition [{}] is triggered. Sending alerts.", alertCondition);

                        // Persist alert.
                        Alert alert = Alert.factory(result, core);
                        alert.save();

                        // Send alerts.
                        if (stream.getAlertReceivers().size() > 0) {
                            try {
                                AlertSender sender = new AlertSender(core);
                                if (alertCondition.getBacklog() > 0 && alertCondition.getSearchHits() != null) {
                                    List<Message> backlog = Lists.newArrayList();

                                    for (ResultMessage searchHit : alertCondition.getSearchHits()) {
                                        backlog.add(new Message(searchHit.message));
                                    }

                                    // Read as many messages as possible (max: backlog size) from backlog.
                                    int readTo = alertCondition.getBacklog();
                                    if(backlog.size() < readTo) {
                                        readTo = backlog.size();
                                    }
                                    sender.sendEmails(stream, result, backlog.subList(0, readTo));
                                } else {
                                    sender.sendEmails(stream, result);
                                }
                            } catch (TransportConfigurationException e) {
                                LOG.warn("Stream [{}] has alert receivers and is triggered, but email transport is not configured.", stream);
                            } catch (Exception e) {
                                LOG.error("Stream [{}] has alert receivers and is triggered, but sending emails failed: ", stream, e);
                            }
                        }
                    } else {
                        // Alert not triggered.
                        LOG.debug("Alert condition [{}] is triggered.", alertCondition);
                    }
                } catch(Exception e) {
                    LOG.error("Skipping alert check that threw an exception.", e);
                    continue;
                }
            }

        }
    }

    @Override
    public boolean runsForever() {
        return false;
    }

    @Override
    public boolean isDaemon() {
        return true;
    }

    @Override
    public boolean stopOnGracefulShutdown() {
        return true;
    }

    @Override
    public boolean masterOnly() {
        return true;
    }

    @Override
    public boolean startOnThisNode() {
        return true;
    }

    @Override
    public int getInitialDelaySeconds() {
        return 10;
    }

    @Override
    public int getPeriodSeconds() {
        return 60;
    }
}

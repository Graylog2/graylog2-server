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

import org.graylog2.Core;
import org.graylog2.alerts.Alert;
import org.graylog2.alerts.AlertCondition;
import org.graylog2.alerts.AlertSender;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class AlertScannerThread implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(AlertScannerThread.class);
    
    public static final int INITIAL_DELAY = 10;
    public static final int PERIOD = 60;
    
    private final Core server;
    
    public AlertScannerThread(Core server) {
        this.server = server;
    }
    
    @Override
    public void run() {
        LOG.debug("Running alert checks.");

        List<Stream> alertedStreams = StreamImpl.loadAllWithConfiguredAlertConditions(server);

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
                        Alert alert = Alert.factory(result, server);
                        alert.save();

                        // Send alerts.
                        AlertSender sender = new AlertSender(server);
                        sender.sendEmails(stream, result);
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

}

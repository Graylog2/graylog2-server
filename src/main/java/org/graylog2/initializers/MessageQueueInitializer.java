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

package org.graylog2.initializers;

import org.graylog2.Configuration;
import org.graylog2.GraylogServer;
import org.graylog2.messagequeue.MessageQueue;
import org.graylog2.periodical.BulkIndexerThread;

/**
 * MessageQueueInitializer.java: Apr 11, 2012 6:49:28 PM
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class MessageQueueInitializer extends SimpleFixedRateScheduleInitializer implements Initializer {

    private Configuration configuration;

    public MessageQueueInitializer(GraylogServer graylogServer, Configuration configuration) {
        this.graylogServer = graylogServer;
        this.configuration = configuration;
    }

    @Override
    public void initialize() {
        // Set the maximum size if it was configured to something else than 0 (= UNLIMITED)
        if (configuration.getMessageQueueMaximumSize() != MessageQueue.SIZE_LIMIT_UNLIMITED) {
            MessageQueue.getInstance().setMaximumSize(configuration.getMessageQueueMaximumSize());
        }

        configureScheduler(
                new BulkIndexerThread(this.graylogServer, configuration),
                BulkIndexerThread.INITIAL_DELAY,
                configuration.getMessageQueuePollFrequency()
        );
    }



}
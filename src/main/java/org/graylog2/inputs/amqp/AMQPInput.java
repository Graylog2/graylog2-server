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
package org.graylog2.inputs.amqp;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.graylog2.Configuration;
import org.graylog2.Core;
import org.graylog2.inputs.MessageInput;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class AMQPInput implements MessageInput {

    public static ExecutorService executor = Executors.newCachedThreadPool(
            new BasicThreadFactory.Builder()
                .namingPattern("amqp-consumer-%d")
                .build()
    );
    
    private static final String NAME = "AMQP syslog/GELF";

    @Override
    public void initialize(Configuration configuration, Core graylogServer) {
        for (AMQPQueueConfiguration config : AMQPQueueConfiguration.fetchAll(graylogServer)) {
            executor.submit(new AMQPConsumer(graylogServer, config));
        }
     }

    @Override
    public String getName() {
        return NAME;
    }
    
}

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

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.elasticsearch.common.collect.Maps;
import org.graylog2.Core;
import org.graylog2.plugin.GraylogServer;
import org.graylog2.plugin.inputs.MessageInput;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class AMQPInput implements MessageInput {

    private static ExecutorService executor = Executors.newCachedThreadPool(
            new ThreadFactoryBuilder()
                .setNameFormat("amqp-input-%d")
                .build()
    );
    
    private static Map<String, AMQPConsumer> consumers;
    
    private static final String NAME = "AMQP syslog/GELF";
    
    public AMQPInput() {
        consumers = Maps.newConcurrentMap();
    }

    @Override
    public void initialize(Map<String, String> configuration, GraylogServer graylogServer) {
        for (AMQPQueueConfiguration config : AMQPQueueConfiguration.fetchAll((Core) graylogServer)) {
            AMQPConsumer consumer = new AMQPConsumer((Core) graylogServer, config);
            executor.submit(consumer);
            consumers.put(config.getId(), consumer);
        }
    }
    
    public static ExecutorService getThreadPool() {
        return executor;
    }
    
    public static Map<String, AMQPConsumer> getConsumers() {
        return consumers;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Map<String, String> getRequestedConfiguration() {
        // Built in input. This is just for plugin compat. No special configuration required.
        return com.google.common.collect.Maps.newHashMap();
    }
    
}

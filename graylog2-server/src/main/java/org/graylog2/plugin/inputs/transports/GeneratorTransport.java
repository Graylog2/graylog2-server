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
package org.graylog2.plugin.inputs.transports;

import com.codahale.metrics.MetricSet;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.Service;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.MisfireException;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.graylog2.plugin.journal.RawMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class GeneratorTransport extends ThrottleableTransport {
    private static final Logger log = LoggerFactory.getLogger(GeneratorTransport.class);

    private Service generatorService;

    public GeneratorTransport(EventBus eventBus, Configuration configuration) {
        super(eventBus, configuration);
    }

    @Override
    public void setMessageAggregator(CodecAggregator aggregator) {

    }

    protected abstract RawMessage produceRawMessage(MessageInput input);

    @Override
    public void doLaunch(final MessageInput input) throws MisfireException {
        generatorService = new AbstractExecutionThreadService() {
            @Override
            protected void run() throws Exception {
                while (isRunning()) {

                    if (isThrottled()) {
                        blockUntilUnthrottled();
                    }
                    final RawMessage rawMessage = GeneratorTransport.this.produceRawMessage(input);
                    if (rawMessage != null) {
                        input.processRawMessage(rawMessage);
                    }
                }
            }
        };

        generatorService.startAsync();
    }

    @Override
    public void doStop() {
        if (generatorService == null || !generatorService.isRunning()) {
            log.error("Cannot stop generator transport, it isn't running.");
            return;
        }
        log.debug("Stopping generator transport service {}", generatorService);
        generatorService.stopAsync().awaitTerminated();
        generatorService = null;
    }

    @Override
    public MetricSet getMetricSet() {
        return null;
    }

    public static class Config extends ThrottleableTransport.Config {
    }
}

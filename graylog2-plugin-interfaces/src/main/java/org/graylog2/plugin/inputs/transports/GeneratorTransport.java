/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.graylog2.plugin.inputs.transports;

import com.codahale.metrics.MetricSet;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.Service;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.MisfireException;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.graylog2.plugin.journal.RawMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class GeneratorTransport implements Transport {
    private static final Logger log = LoggerFactory.getLogger(GeneratorTransport.class);

    private Service generatorService;

    @Override
    public void setMessageAggregator(CodecAggregator aggregator) {

    }

    protected abstract RawMessage produceRawMessage(MessageInput input);

    @Override
    public void launch(final MessageInput input) throws MisfireException {
        generatorService = new AbstractExecutionThreadService() {
            @Override
            protected void run() throws Exception {
                while (isRunning()) {

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
    public void stop() {
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

    public static class Config implements Transport.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            return new ConfigurationRequest();
        }
    }
}

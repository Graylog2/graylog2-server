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
package org.graylog2.inputs.raw;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.common.util.concurrent.TimeLimiter;
import org.elasticsearch.common.inject.assistedinject.AssistedInject;
import org.graylog2.plugin.BaseConfiguration;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.buffers.Buffer;
import org.graylog2.plugin.buffers.BufferOutOfCapacityException;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class RawProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(RawProcessor.class);
    private final Buffer processBuffer;
    private final Configuration config;

    private final MessageInput sourceInput;

    private final Meter incomingMessages;
    private final Meter failures;
    private final Meter incompleteMessages;
    private final Meter processedMessages;
    private final Timer parseTime;
    private final Timer resolveTime;
    private final Meter resolveTimeouts;

    private final ExecutorService executor;
    private final TimeLimiter timeLimiter;

    public RawProcessor(MetricRegistry metricRegistry,
                        Buffer processBuffer,
                        Configuration config,
                        MessageInput sourceInput) {
        this.processBuffer = processBuffer;
        this.config = config;

        this.sourceInput = sourceInput;

        String metricName = sourceInput.getUniqueReadableId();
        this.incomingMessages = metricRegistry.meter(name(metricName, "incomingMessages"));
        this.failures = metricRegistry.meter(name(metricName, "failures"));
        this.processedMessages = metricRegistry.meter(name(metricName, "processedMessages"));
        this.incompleteMessages = metricRegistry.meter(name(metricName, "incompleteMessages"));
        this.parseTime = metricRegistry.timer(name(metricName, "parseTime"));
        this.resolveTime = metricRegistry.timer(name(metricName, "resolveTime"));
        this.resolveTimeouts = metricRegistry.meter(name(metricName, "resolveTimeouts"));

        this.executor = Executors.newCachedThreadPool(
                new ThreadFactoryBuilder()
                        .setNameFormat("raw-processor-%d")
                        .setDaemon(true)
                        .build()
        );
        this.timeLimiter = new SimpleTimeLimiter(executor);
    }

    public void messageReceived(String msg, InetAddress remoteAddress) throws BufferOutOfCapacityException {
        incomingMessages.mark();

        // Convert to LogMessage
        Message lm;
        try {
            lm = new Message(msg, parseSource(msg, remoteAddress), Tools.iso8601());
        } catch (Exception e) {
            failures.mark();
            LOG.error("Could not parse raw message. Not further handling.", e);
            return;
        }

        if (!lm.isComplete()) {
            incompleteMessages.mark();
            LOG.debug("Skipping incomplete message.");
            return;
        }

        // Add to process buffer.
        LOG.debug("Adding received raw message <{}> to process buffer: {}", lm.getId(), lm);
        processedMessages.mark();
        processBuffer.insertCached(lm, sourceInput);
    }

    private String parseSource(String msg, final InetAddress remoteAddress) {
        final String result;
        final long timeout;

        if (config.getSource().containsKey(RawInputBase.CK_DNS_TIMEOUT))
            timeout = config.getInt(RawInputBase.CK_DNS_TIMEOUT);
        else
            timeout = RawInputBase.CK_DNS_TIMEOUT_DEFAULT;

        if (config.stringIsSet(RawInputBase.CK_OVERRIDE_SOURCE)) {
            result = config.getString(RawInputBase.CK_OVERRIDE_SOURCE);
        } else {
            Callable<String> task = new Callable<String>() {
                @Override
                public String call() throws Exception {
                    return remoteAddress.getCanonicalHostName();
                }
            };
            try (Timer.Context context = this.resolveTime.time()) {
                result = timeLimiter.callWithTimeout(task, timeout, TimeUnit.MILLISECONDS, true);
            } catch (TimeoutException e) {
                LOG.error("DNS lookup timed out for record: {}, timeout {}ms", remoteAddress.getHostAddress(), timeout);
                this.resolveTimeouts.mark();
                return remoteAddress.getHostAddress();
            } catch (Exception e) {
                LOG.error("Error occured during name resolution:", e);
                return remoteAddress.getHostAddress();
            }
        }

        return result;
    }

}

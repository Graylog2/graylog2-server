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

package org.graylog2.shared.buffers.processors;

import com.codahale.metrics.Timer;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.net.InetAddresses;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lmax.disruptor.EventHandler;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.ResolvableInetSocketAddress;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.buffers.MessageEvent;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.shared.inputs.InputRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DecodingProcessor implements EventHandler<MessageEvent> {
    private static final Logger log = LoggerFactory.getLogger(DecodingProcessor.class);

    private final LoadingCache<String, MessageInput> inputCache;

    public interface Factory {
        public DecodingProcessor create(@Assisted("parseTime") Timer parseTime);
    }

    private final Map<String, Codec.Factory<? extends Codec>> codecFactory;
    private final InputRegistry inputRegistry;
    private final ServerStatus serverStatus;
    private final Timer parseTime;

    @AssistedInject
    public DecodingProcessor(Map<String, Codec.Factory<? extends Codec>> codecFactory,
                             final InputRegistry inputRegistry,
                             final ServerStatus serverStatus,
                             @Assisted("parseTime") Timer parseTime) {
        this.codecFactory = codecFactory;
        this.inputRegistry = inputRegistry;
        this.serverStatus = serverStatus;

        /*
        failures = localRegistry.meter("failures");
        incompleteMessages = localRegistry.meter("incompleteMessages");
        rawSize = localRegistry.meter("rawSize");
        */
        this.parseTime = parseTime;

        // Use cache here to avoid looking up the inputs in the InputRegistry for every message.
        // TODO Check if there is a better way to do this!
        this.inputCache = CacheBuilder.newBuilder()
                .expireAfterAccess(1, TimeUnit.SECONDS)
                .build(new CacheLoader<String, MessageInput>() {
                    @Override
                    public MessageInput load(String inputId) throws Exception {
                        return inputRegistry.getRunningInput(inputId);
                    }
                });
    }

    @Override
    public void onEvent(MessageEvent event, long sequence, boolean endOfBatch) throws Exception {
        final RawMessage raw = event.getRaw();
        if (raw == null) {
            log.warn("Ignoring null message");
            return;
        }
        final Codec codec = codecFactory.get(raw.getCodecName()).create(raw.getCodecConfig());

        /*


        if (message == null) {
            failures.mark();
            LOG.warn("Could not decode message. Dropping message {}", rawMessage.getId());
            return;
        }
        if (!message.isComplete()) {
            incompleteMessages.mark();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Dropping incomplete message. Parsed fields: [{}]", message.getFields());
            }
            return;
        }

        processedMessages.mark();
        */

        final Message message;

        // TODO Create parse times per codec as well. (add some more metrics too)
        try (Timer.Context ignored = parseTime.time()) {
            message = codec.decode(raw);
            message.setJournalOffset(raw.getJournalOffset());
        } catch (RuntimeException e) {
            throw e;
            //failures.mark();
        }

        if (message == null) {
            return;
        }

        for (final RawMessage.SourceNode node : raw.getSourceNodes()) {
            switch (node.type) {
                case SERVER:
                    // Currently only one of each type supported at the moment.
                    if (message.getField("gl2_source_input") != null) {
                        throw new IllegalStateException("Multiple server nodes");
                    }
                    message.addField("gl2_source_input", node.inputId);
                    message.addField("gl2_source_node", node.nodeId);
                    break;
                case RADIO:
                    // Currently only one of each type supported at the moment.
                    if (message.getField("gl2_source_radio_input") != null) {
                        throw new IllegalStateException("Multiple radio nodes");
                    }
                    message.addField("gl2_source_radio_input", node.inputId);
                    message.addField("gl2_source_radio", node.nodeId);
                    break;
            }
        }

        for (RawMessage.SourceNode node : raw.getSourceNodes()) {
            if (serverStatus.getNodeId().toString().equals(node.nodeId) && node.inputId != null) {
                message.setSourceInput(inputCache.get(node.inputId));
                break;
            }
        }

        final ResolvableInetSocketAddress remoteAddress = raw.getRemoteAddress();
        if (remoteAddress != null) {
            message.addField("gl2_remote_ip", InetAddresses.toAddrString(remoteAddress.getAddress()));
            if (remoteAddress.getPort() > 0) {
                message.addField("gl2_remote_port", remoteAddress.getPort());
            }
            if (remoteAddress.isReverseLookedUp()) { // avoid reverse lookup if the hostname is available
                message.addField("gl2_remote_hostname", remoteAddress.getHostName());
            }
        }
        event.setMessage(message);
    }
}

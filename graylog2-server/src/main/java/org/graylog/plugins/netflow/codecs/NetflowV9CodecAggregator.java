/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.plugins.netflow.codecs;

import com.github.joschi.jadconfig.util.Size;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.protobuf.ByteString;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.graylog.plugins.netflow.v9.NetFlowV9Journal;
import org.graylog.plugins.netflow.v9.NetFlowV9Parser;
import org.graylog.plugins.netflow.v9.RawNetFlowV9Packet;
import org.graylog2.shared.utilities.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * For Netflow v9 packets we want to prepend the corresponding flow template.
 * If we don't have that template yet, we consider the flow packet to be incomplete and continue to wait for the template.
 * TODO consider sharing seen templates between nodes in the cluster to minimize wait time
 */
public class NetflowV9CodecAggregator implements RemoteAddressCodecAggregator {
    private static final Logger LOG = LoggerFactory.getLogger(NetflowV9CodecAggregator.class);

    private static final ByteBuf PASSTHROUGH_MARKER = Unpooled.wrappedBuffer(new byte[]{NetFlowCodec.PASSTHROUGH_MARKER});

    private final Cache<TemplateKey, TemplateBytes> templateCache;
    private final Cache<TemplateKey, Queue<PacketBytes>> packetCache;

    @Inject
    public NetflowV9CodecAggregator() {
        // TODO customize
        this.templateCache = CacheBuilder.newBuilder()
                .maximumSize(5000)
                .removalListener(notification -> LOG.debug("Removed {} from template cache for reason {}", notification.getKey(), notification.getCause()))
                .recordStats()
                .build();
        this.packetCache = CacheBuilder.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .maximumWeight(Size.megabytes(1).toBytes())
                .removalListener((RemovalListener<TemplateKey, Queue<PacketBytes>>) notification -> LOG.debug("Removed {} from packet cache for reason {}", notification.getKey(), notification.getCause()))
                .weigher((key, value) -> value.stream().map(PacketBytes::readableBytes).reduce(0, Integer::sum))
                .recordStats()
                .build();
    }

    @Nonnull
    @Override
    public Result addChunk(ByteBuf buf, SocketAddress remoteAddress) {
        if (buf.readableBytes() < 2) {
            // the buffer doesn't contain enough bytes to be a netflow packet, discard the packet
            return new Result(null, false);
        }

        // This thing is using *WAY* too many copies. :'(
        try {
            final int netFlowVersion = buf.getShort(0);

            // only netflow v9 needs special treatment, everything else we just pass on
            if (netFlowVersion != 9) {
                return new Result(Unpooled.copiedBuffer(PASSTHROUGH_MARKER, buf), true);
            }

            // for NetFlow V9 we check that we have previously received template flows for each data flow.
            // if we do not have them yet, buffer the data flows until we receive a matching template
            // since we do not want to do that again in the codec, we will violate the RFC when putting together
            // the packets again:
            // the codec can, contrary to https://tools.ietf.org/html/rfc3954#section-9, assume that for each packet/RawMessage
            // the packet contains all necessary templates. This greatly simplifies parsing at the expense of larger RawMessages.

            // The rest of the code works as follows:
            // We shallowly parse the incoming packet, extracting all flows into ByteBufs.
            // We then cache the raw bytes for template flows, keyed by remote ip and source id. These are used to reassemble the packet for the journal later.
            // For each netflow v9 packet that we do not have a matching template for yet, we put it into a queue.
            // Once the template flow arrives we go back through the queue and remove now matching packets for further processing.
            if (LOG.isTraceEnabled()) {
                LOG.trace("Received V9 packet:\n{}", ByteBufUtil.prettyHexDump(buf));
            }
            final RawNetFlowV9Packet rawNetFlowV9Packet = NetFlowV9Parser.parsePacketShallow(buf);
            final long sourceId = rawNetFlowV9Packet.header().sourceId();

            LOG.trace("Incoming NetFlow V9 packet contains: {}", rawNetFlowV9Packet);

            // register templates and check for buffered flows
            for (Map.Entry<Integer, byte[]> template : rawNetFlowV9Packet.templates().entrySet()) {
                final int templateId = template.getKey();
                final byte[] bytes = template.getValue();

                final TemplateKey templateKey = new TemplateKey(remoteAddress, sourceId, templateId);
                final TemplateBytes templateBytes = new TemplateBytes(bytes, false);
                templateCache.put(templateKey, templateBytes);
            }

            final Map.Entry<Integer, byte[]> optionTemplate = rawNetFlowV9Packet.optionTemplate();
            if (optionTemplate != null) {
                final int templateId = optionTemplate.getKey();
                final byte[] bytes = optionTemplate.getValue();

                final TemplateKey templateKey = new TemplateKey(remoteAddress, sourceId, templateId);
                final TemplateBytes templateBytes = new TemplateBytes(bytes, true);

                templateCache.put(templateKey, templateBytes);
            }

            // this list of flows to return in the result
            // Using ByteBuf here to enable de-duplication with the hash set.
            final Set<ByteBuf> packetsToSend = new HashSet<>();
            final Set<Integer> bufferedTemplateIds = new HashSet<>();

            // if we have new templates, figure out which buffered packets template requirements are now satisfied
            if (!rawNetFlowV9Packet.templates().isEmpty() || rawNetFlowV9Packet.optionTemplate() != null) {
                final Set<Integer> knownTemplateIds = new HashSet<>();
                for (TemplateKey templateKey : templateCache.asMap().keySet()) {
                    if (templateKey.getRemoteAddress() == remoteAddress && templateKey.getSourceId() == sourceId) {
                        final Integer templateId = templateKey.getTemplateId();
                        knownTemplateIds.add(templateId);
                    }
                }

                final Queue<PacketBytes> bufferedPackets = packetCache.getIfPresent(TemplateKey.idForExporter(remoteAddress, sourceId));
                if (bufferedPackets != null) {
                    final List<PacketBytes> tempQueue = new ArrayList<>(bufferedPackets.size());
                    PacketBytes previousPacket;
                    int addedPackets = 0;
                    while (null != (previousPacket = bufferedPackets.poll())) {
                        // are all templates the packet references there?
                        if (knownTemplateIds.containsAll(previousPacket.getUsedTemplates())) {
                            packetsToSend.add(Unpooled.wrappedBuffer(previousPacket.getBytes()));
                            bufferedTemplateIds.addAll(previousPacket.getUsedTemplates());
                            addedPackets++;
                        } else {
                            tempQueue.add(previousPacket);
                        }
                    }
                    LOG.debug("Processing {} previously buffered packets, {} packets require more templates.", addedPackets, tempQueue.size());
                    // if we couldn't process some of the buffered packets, add them back to the queue to wait for more templates to come in
                    if (!tempQueue.isEmpty()) {
                        bufferedPackets.addAll(tempQueue);
                    }
                }
            }

            boolean packetBuffered = false;

            // the list of template keys to return in the result
            final Set<TemplateKey> templates = new HashSet<>();

            // find out which templates we need to include for the buffered and current packets
            bufferedTemplateIds.addAll(rawNetFlowV9Packet.usedTemplates());
            for (int templateId : bufferedTemplateIds) {
                final TemplateKey templateKey = new TemplateKey(remoteAddress, sourceId, templateId);
                final TemplateBytes template = templateCache.getIfPresent(templateKey);

                if (template == null) {
                    // we don't have the template, this packet needs to be buffered until we receive the templates
                    try {
                        final TemplateKey newTemplateKey = TemplateKey.idForExporter(remoteAddress, sourceId);
                        final Queue<PacketBytes> bufferedPackets = packetCache.get(newTemplateKey, ConcurrentLinkedQueue::new);
                        final byte[] bytes = ByteBufUtil.getBytes(buf);
                        bufferedPackets.add(new PacketBytes(bytes, rawNetFlowV9Packet.usedTemplates()));
                        packetBuffered = true;
                    } catch (ExecutionException ignored) {
                        // the loader cannot fail, it only creates a new queue
                    }
                } else {
                    // include the template in our result
                    templates.add(templateKey);

                    // .slice is enough here, because we convert it into a byte array when creating the result below
                    // no need to copy or retain anything, the buffer only lives as long as this method's scope
                    final ByteBuf packet = buf.slice();
                    packetsToSend.add(packet);
                }
            }

            // if we have buffered this packet, don't try to process it now. we still need all the templates for it
            if (packetBuffered) {
                return new Result(null, true);
            }

            // if we didn't buffer anything but also didn't have anything queued that can be processed, don't proceed.
            if (packetsToSend.isEmpty()) {
                return new Result(null, true);
            }

            // add the used templates and option template to the journal message builder
            final NetFlowV9Journal.RawNetflowV9.Builder builder = NetFlowV9Journal.RawNetflowV9.newBuilder();
            for (TemplateKey templateKey : templates) {
                final TemplateBytes templateBytes = templateCache.getIfPresent(templateKey);
                if (templateBytes == null) {
                    LOG.warn("Template {} expired while processing, discarding netflow packet", templateKey);
                } else if (templateBytes.isOptionTemplate()) {
                    LOG.debug("Writing options template flow {}", templateKey);
                    final byte[] bytes = templateBytes.getBytes();
                    builder.putOptionTemplate(1, ByteString.copyFrom(bytes));
                } else {
                    LOG.debug("Writing template {}", templateKey);
                    final byte[] bytes = templateBytes.getBytes();
                    builder.putTemplates(templateKey.getTemplateId(), ByteString.copyFrom(bytes));
                }
            }

            // finally write out all the packets we had buffered as well as the current one
            for (ByteBuf packetBuffer : packetsToSend) {
                final byte[] bytes = ByteBufUtil.getBytes(packetBuffer);
                final ByteString value = ByteString.copyFrom(bytes);
                builder.addPackets(value);
            }

            final byte[] bytes = builder.build().toByteArray();
            final ByteBuf resultBuffer = Unpooled.buffer(bytes.length + 1)
                    .writeByte(NetFlowCodec.ORDERED_V9_MARKER)
                    .writeBytes(bytes);
            return new Result(resultBuffer, true);

        } catch (Exception e) {
            LOG.error("Unexpected failure while aggregating NetFlowV9 packet, discarding packet.", ExceptionUtils.getRootCause(e));
            return new Result(null, false);
        }
    }

    private static class TemplateBytes {
        private final byte[] bytes;
        private final boolean optionTemplate;

        public TemplateBytes(byte[] bytes, boolean optionTemplate) {
            this.bytes = bytes;
            this.optionTemplate = optionTemplate;
        }

        public byte[] getBytes() {
            return bytes;
        }

        public boolean isOptionTemplate() {
            return optionTemplate;
        }
    }

    public static class PacketBytes {
        private final byte[] bytes;
        private final Set<Integer> usedTemplates;

        public PacketBytes(byte[] bytes, Set<Integer> usedTemplates) {
            this.bytes = bytes;
            this.usedTemplates = usedTemplates;
        }

        public byte[] getBytes() {
            return bytes;
        }

        public Set<Integer> getUsedTemplates() {
            return usedTemplates;
        }

        public int readableBytes() {
            return bytes.length;
        }
    }
}

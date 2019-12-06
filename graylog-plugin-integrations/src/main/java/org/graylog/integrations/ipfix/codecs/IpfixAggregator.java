/**
 * This file is part of Graylog.
 * <p>
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.integrations.ipfix.codecs;

import com.github.joschi.jadconfig.util.Size;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.protobuf.ByteString;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.graylog.integrations.ipfix.InformationElementDefinitions;
import org.graylog.integrations.ipfix.IpfixJournal;
import org.graylog.integrations.ipfix.IpfixParser;
import org.graylog.integrations.ipfix.ShallowDataSet;
import org.graylog.integrations.ipfix.ShallowTemplateSet;
import org.graylog.plugins.netflow.codecs.RemoteAddressCodecAggregator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class IpfixAggregator implements RemoteAddressCodecAggregator {
    private static final Logger LOG = LoggerFactory.getLogger(IpfixAggregator.class);
    private final Cache<TemplateKey, ShallowTemplateSet.Record> templateCache;
    private final Cache<TemplateKey, Queue<ShallowDataSet>> packetCache;
    private final IpfixParser shallowParser = new IpfixParser(InformationElementDefinitions.empty());

    public IpfixAggregator() {
        this.templateCache = CacheBuilder.newBuilder()
                                         .maximumSize(5000)
                                         .removalListener(notification -> LOG.debug("Removed [{}] from template cache for reason [{}]", notification.getKey(), notification.getCause()))
                                         .recordStats()
                                         .build();
        this.packetCache = CacheBuilder.newBuilder()
                                       .expireAfterWrite(1, TimeUnit.MINUTES)
                                       .maximumWeight(Size.megabytes(1).toBytes())
                                       .removalListener((RemovalListener<TemplateKey, Queue<ShallowDataSet>>) notification -> LOG.debug("Removed [{}] from packet cache for reason [{}]", notification.getKey(), notification.getCause()))
                                       .weigher((key, value) -> value.stream().map(shallowDataSet -> shallowDataSet.content().length).reduce(0, Integer::sum))
                                       .recordStats()
                                       .build();
    }

    @Nonnull
    @Override
    public Result addChunk(ByteBuf buf, @Nullable SocketAddress remoteAddress) {
        if (!buf.isReadable(2)) {
            return new Result(null, false);
        }
        try {
            final IpfixParser.MessageDescription messageDescription = shallowParser.shallowParseMessage(buf);
            final long observationDomainId = messageDescription.getHeader().observationDomainId();
            addTemplateKeyInCache(remoteAddress, messageDescription, observationDomainId);
            // TODO handle options templates

            // collects all data records that are now ready to be sent
            final Set<ShallowDataSet> packetsToSendCollection = new HashSet<>();

            // the set of template records to include in the newly created message that is our "aggregate result"
            final Set<Integer> bufferedTemplateIdList = new HashSet<>();

            if (!messageDescription.declaredTemplateIds().isEmpty()) {
                // if we have new templates, look for buffered data records that we have all the templates for now
                final Set<Integer> knownTemplateIdsList = new HashSet<>();
                collectAllTemplateIds(remoteAddress, observationDomainId, knownTemplateIdsList);

                final Queue<ShallowDataSet> bufferedPackets = packetCache.getIfPresent(TemplateKey.idForExporter(remoteAddress, observationDomainId));
                handleBufferedPackets(packetsToSendCollection, bufferedTemplateIdList, knownTemplateIdsList, bufferedPackets);
            }
            boolean packetBuffered = false;

            // the list of template keys to return in the result
            final Set<TemplateKey> templatesList = new HashSet<>();

            bufferedTemplateIdList.addAll(messageDescription.referencedTemplateIds());
            LOG.debug("Finding the needed templates for the buffered and current packets");
            for (int templateId : bufferedTemplateIdList) {
                final TemplateKey templateKey = new TemplateKey(remoteAddress, observationDomainId, templateId);
                final Object template = templateCache.getIfPresent(templateKey);

                if (template == null) {
                    LOG.debug("Template is null, packet needs to be buffered until templates have been received.");
                    try {
                        final TemplateKey newTemplateKey = TemplateKey.idForExporter(remoteAddress, observationDomainId);
                        final Queue<ShallowDataSet> bufferedPackets = packetCache.get(newTemplateKey, ConcurrentLinkedQueue::new);
                        final byte[] bytes = ByteBufUtil.getBytes(buf);
                        bufferedPackets.addAll(messageDescription.dataSets());
                        packetBuffered = true;
                    } catch (ExecutionException ignored) {
                        // the loader cannot fail, it only creates a new queue
                    }
                } else {
                    LOG.debug("Template [{}] has been added to template list.", templateKey);
                    templatesList.add(templateKey);
                    packetsToSendCollection.addAll(messageDescription.dataSets());
                }
            }

            // if we have buffered this packet, don't try to process it now. we still need all the templates for it
            if (packetBuffered) {
                LOG.debug("Packet has been buffered and will not be processed now, returning result.");
                return new Result(null, true);
            }

            // if we didn't buffer anything but also didn't have anything queued that can be processed, don't proceed.
            if (packetsToSendCollection.isEmpty()) {
                LOG.debug("Packet has not been buffered and no packet is queued.");
                return new Result(null, true);
            }

            final IpfixJournal.RawIpfix.Builder journalBuilder = IpfixJournal.RawIpfix.newBuilder();
            buildJournalObject(packetsToSendCollection, templatesList, journalBuilder);
            final IpfixJournal.RawIpfix rawIpfix = journalBuilder.build();
            return getCompleteResult(rawIpfix);

        } catch (Exception e) {
            LOG.error("Unable to aggregate IPFIX message due to the following error ", e);
            return new Result(null, false);
        }
    }

    public void buildJournalObject(Set<ShallowDataSet> packetsToSendCollection, Set<TemplateKey> templatesList, IpfixJournal.RawIpfix.Builder journalBuilder) {
        LOG.debug("Assembling the packet with necessary templates and data records which include the templates needed.");
        for (TemplateKey templateKey : templatesList) {
            final ShallowTemplateSet.Record record = templateCache.getIfPresent(templateKey);
            journalBuilder.putTemplates(templateKey.getTemplateId(), ByteString.copyFrom(record.getRecordBytes()));
        }

        // TODO write out options template sets, too

        // in IPFIX a data set contains records for the same template id, so we can just dump the entire set and don't
        // have to deal with records at all
        LOG.debug("IPFIX data set has been processed for the same template id, adding data set to IPFIX journal.");
        for (ShallowDataSet dataSet : packetsToSendCollection) {
            journalBuilder.addDataSets(IpfixJournal.DataSet.newBuilder()
                                                           .setTemplateId(dataSet.templateId())
                                                           .setTimestampEpochSeconds(dataSet.epochSeconds())
                                                           .setDataRecords(ByteString.copyFrom(dataSet.content()))
                                                           .build());
        }
    }

    public Result getCompleteResult(IpfixJournal.RawIpfix rawIpfix) {
        LOG.debug("Raw ipfix object complete, returning result.");
        return new Result(Unpooled.wrappedBuffer(rawIpfix.toByteArray()), true);
    }

    public void handleBufferedPackets(Set<ShallowDataSet> packetsToSendCollection, Set<Integer> bufferedTemplateIdList,
                                      Set<Integer> knownTemplateIdsList, Queue<ShallowDataSet> bufferedPackets) {
        if (bufferedPackets != null) {
            LOG.debug("Buffered packets detected in the packet cache.");
            final List<ShallowDataSet> tempQueue = new ArrayList<>(bufferedPackets.size());
            ShallowDataSet previousPacket;
            int addedPackets = 0;
            while (null != (previousPacket = bufferedPackets.poll())) {
                // are all templates the packet references there?
                if (knownTemplateIdsList.contains(previousPacket.templateId())) {
                    LOG.debug("Packet contains template id from a known template, adding to packets to send set.");
                    packetsToSendCollection.add(previousPacket);
                    bufferedTemplateIdList.add(previousPacket.templateId());
                    addedPackets++;
                } else {
                    LOG.debug("Packet contains unknown template id, adding to temporary queue.");
                    tempQueue.add(previousPacket);
                }
            }
            LOG.debug("Processing [{}] previously buffered packets, [{}] packets require more templates.", addedPackets, tempQueue.size());
            // if we couldn't process some of the buffered packets, add them back to the queue to wait for more templates to come in
            if (!tempQueue.isEmpty()) {
                LOG.debug("Buffered packets could not be processed, adding to temporary queue to wait for more templates.");
                bufferedPackets.addAll(tempQueue);
            }
        }
    }

    public void collectAllTemplateIds(@Nullable SocketAddress remoteAddress, long observationDomainId, Set<Integer> knownTemplateIdsList) {
        LOG.debug("Collecting all templateIds from templateKeys stored in templateCache.");
        for (TemplateKey templateKey : templateCache.asMap().keySet()) {
            if (templateKey.getRemoteAddress() == remoteAddress && templateKey.getObservationDomainId() == observationDomainId) {
                final Integer templateId = templateKey.getTemplateId();
                knownTemplateIdsList.add(templateId);
            }
        }
    }

    public void addTemplateKeyInCache(@Nullable SocketAddress remoteAddress, IpfixParser.MessageDescription messageDescription, long observationDomainId) {
        for (Integer templateId : messageDescription.declaredTemplateIds()) {
            final TemplateKey templateKey = new TemplateKey(remoteAddress, observationDomainId, templateId);
            LOG.debug("Created template key with remote address [{}], observation domain ID [{}] and " +
                      "template ID [{}].", templateKey.getRemoteAddress(), templateKey.getObservationDomainId(), templateKey.getTemplateId());

            templateCache.put(templateKey, messageDescription.getTemplateRecord(templateId));
            LOG.debug("Saving templates key (raw bytes) in template cache to combine in new message later.");
        }
    }
}

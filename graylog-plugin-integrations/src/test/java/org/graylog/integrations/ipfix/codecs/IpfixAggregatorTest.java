package org.graylog.integrations.ipfix.codecs;

import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.pkts.Pcap;
import io.pkts.packet.UDPPacket;
import io.pkts.protocol.Protocol;
import org.assertj.core.util.Lists;
import org.graylog.integrations.ipfix.InformationElementDefinitions;
import org.graylog.integrations.ipfix.IpfixMessage;
import org.graylog.integrations.ipfix.IpfixParser;
import org.graylog.integrations.ipfix.Utils;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.graylog2.plugin.journal.RawMessage;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class IpfixAggregatorTest {
    private static final Logger LOG = LoggerFactory.getLogger(IpfixAggregatorTest.class);
    private final InetSocketAddress someAddress = InetSocketAddress.createUnresolved("192.168.1.1", 999);

    private InformationElementDefinitions standardDefinition = new InformationElementDefinitions(
            Resources.getResource("ipfix-iana-elements.json")
    );

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Ignore("Not ready. Has InvalidIPFixMessageVersion.")
    @Test
    public void completePacket() throws IOException {
        final ByteBuf packetBytes = Utils.readPacket("templates-data.ipfix");

        final IpfixAggregator aggregator = new IpfixAggregator();
        final CodecAggregator.Result result = aggregator.addChunk(packetBytes, someAddress);

        assertThat(result).isNotNull();
        assertThat(result.isValid()).isTrue();
        assertThat(result.getMessage()).isNotNull();

        final IpfixMessage ipfixMessage = new IpfixParser(standardDefinition).parseMessage(result.getMessage());
        assertThat(ipfixMessage).isNotNull();
    }

    @Ignore("Not ready. Has InvalidIPFixMessageVersion.")
    @Test
    public void multipleMessagesTemplateLater() throws IOException {
        final ByteBuf datasetOnlyBytes = Utils.readPacket("dataset-only.ipfix");
        final ByteBuf withTemplatesBytes = Utils.readPacket("templates-data.ipfix");
        final IpfixAggregator ipfixAggregator = new IpfixAggregator();
        final CodecAggregator.Result resultQueued = ipfixAggregator.addChunk(datasetOnlyBytes, someAddress);
        assertThat(resultQueued.isValid()).isTrue();
        assertThat(resultQueued.getMessage()).isNull();

        final CodecAggregator.Result resultComplete = ipfixAggregator.addChunk(withTemplatesBytes, someAddress);
        assertThat(resultComplete.isValid()).isTrue();
        assertThat(resultComplete.getMessage()).isNotNull();

        final IpfixMessage ipfixMessage = new IpfixParser(standardDefinition).parseMessage(resultComplete.getMessage());
        assertThat(ipfixMessage.flows()).hasSize(4);
    }

    @Test
    public void dataAndDataTemplate() throws IOException {

        final IpfixAggregator ipfixAggregator = new IpfixAggregator();
        final Map<String, Object> configMap = getIxiaConfigmap();
        final Configuration configuration = new Configuration(configMap);

        final IpfixCodec codec = new IpfixCodec(configuration, ipfixAggregator);

        AtomicInteger messageCount = new AtomicInteger();
        try (InputStream stream = Resources.getResource("data-datatemplate.pcap").openStream()) {
            final Pcap pcap = Pcap.openStream(stream);
            pcap.loop(packet -> {
                if (packet.hasProtocol(Protocol.UDP)) {
                    final UDPPacket udp = (UDPPacket) packet.getPacket(Protocol.UDP);
                    final InetSocketAddress source = new InetSocketAddress(udp.getParentPacket().getSourceIP(), udp.getSourcePort());
                    byte[] payload = new byte[udp.getPayload().getReadableBytes()];
                    udp.getPayload().getBytes(payload);
                    final ByteBuf buf = Unpooled.wrappedBuffer(payload);
                    final CodecAggregator.Result result = ipfixAggregator.addChunk(buf, source);
                    final ByteBuf ipfixRawBuf = result.getMessage();
                    if (ipfixRawBuf != null) {
                        byte[] bytes = new byte[ipfixRawBuf.readableBytes()];
                        ipfixRawBuf.getBytes(0, bytes);
                        final Collection<Message> messages = codec.decodeMessages(new RawMessage(bytes));
                        if (messages != null) {
                            messageCount.addAndGet(messages.size());
                        }
                    }
                }
                return true;
            });
        } catch (IOException e) {
            LOG.debug("Cannot process PCAP stream", e);
        }
        assertThat(messageCount.get()).isEqualTo(4L);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void ixFlowTest() throws IOException {
        final IpfixAggregator ipfixAggregator = new IpfixAggregator();
        final Map<String, Object> configMap = getIxiaConfigmap();
        final IpfixCodec codec = new IpfixCodec(new Configuration(configMap), ipfixAggregator);
        final List<Message> messages = Lists.newArrayList();

        // ixflow.pcap contains 4 packets, the first has the data templates and option templates
        // followed by three data sets. two sets have subtemplateList data, the third has only empty lists for domain information
        try (InputStream stream = Resources.getResource("ixflow.pcap").openStream()) {
            final Pcap pcap = Pcap.openStream(stream);
            pcap.loop(packet -> {
                if (packet.hasProtocol(Protocol.UDP)) {
                    final UDPPacket udp = (UDPPacket) packet.getPacket(Protocol.UDP);
                    final InetSocketAddress source = new InetSocketAddress(udp.getParentPacket().getSourceIP(), udp.getSourcePort());
                    byte[] payload = new byte[udp.getPayload().getReadableBytes()];
                    udp.getPayload().getBytes(payload);
                    final ByteBuf buf = Unpooled.wrappedBuffer(payload);
                    final CodecAggregator.Result result = ipfixAggregator.addChunk(buf, source);
                    final ByteBuf ipfixRawBuf = result.getMessage();
                    if (ipfixRawBuf != null) {
                        byte[] bytes = new byte[ipfixRawBuf.readableBytes()];
                        ipfixRawBuf.getBytes(0, bytes);
                        messages.addAll(Objects.requireNonNull(codec.decodeMessages(new RawMessage(bytes))));
                    }
                }
                return true;
            });
        } catch (IOException e) {
            fail("Cannot process PCAP stream");
        }

        assertThat(messages).hasSize(3);
        assertThat(messages.get(0).getFields())
                .doesNotContainKey("httpSession")
                .containsEntry("dnsRecord_0_dnsIpv4Address", "1.2.0.2")
                .containsEntry("dnsRecord_0_dnsIpv6Address", "0:0:0:0:0:0:0:0")
                .containsEntry("dnsRecord_0_dnsName", "server-1020002.example.int.");
        assertThat(messages.get(1).getFields())
                .doesNotContainKey("httpSession")
                .containsEntry("dnsRecord_0_dnsIpv4Address", "1.2.14.73")
                .containsEntry("dnsRecord_0_dnsIpv6Address", "0:0:0:0:0:0:0:0")
                .containsEntry("dnsRecord_0_dnsName", "server-1020e49.example.int.");
        assertThat(messages.get(2).getFields())
                .doesNotContainKey("httpSession")
                .doesNotContainKey("dnsRecord");

    }

    private Map<String, Object> getIxiaConfigmap() {
        final File filePath = new File(Resources.getResource("ixia-ied.json").getFile());
        final Map<String, Object> configMap = Maps.newHashMap();
        configMap.put(IpfixCodec.CK_IPFIX_DEFINITION_PATH, Collections.singletonList(filePath.getAbsolutePath()));
        return configMap;
    }

}
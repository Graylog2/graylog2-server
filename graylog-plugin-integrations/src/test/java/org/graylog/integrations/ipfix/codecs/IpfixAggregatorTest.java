package org.graylog.integrations.ipfix.codecs;

import com.google.common.io.Resources;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.pkts.Pcap;
import io.pkts.packet.UDPPacket;
import io.pkts.protocol.Protocol;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

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
        final File tempFile = temporaryFolder.newFile("tempFile.json");
        //load the custom definition file
        String custDefStr = "{ \"enterprise_number\": 3054, \"information_elements\": [ { \"element_id\": 110, " +
                            "\"name\": \"l7ApplicationId\", \"data_type\": \"unsigned32\" }, { \"element_id\": 111, " +
                            "\"name\": \"l7ApplicationName\", \"data_type\": \"string\" }, { \"element_id\": 120, " +
                            "\"name\": \"sourceIpCountryCode\", \"data_type\": \"string\" }, { \"element_id\": 121, " +
                            "\"name\": \"sourceIpCountryName\", \"data_type\": \"string\" }, { \"element_id\": 122, " +
                            "\"name\": \"sourceIpRegionCode\", \"data_type\": \"string\" }, { \"element_id\": 123, " +
                            "\"name\": \"sourceIpRegionName\", \"data_type\": \"string\" }, { \"element_id\": 125, " +
                            "\"name\": \"sourceIpCityName\", \"data_type\": \"string\" }, { \"element_id\": 126, " +
                            "\"name\": \"sourceIpLatitude\", \"data_type\": \"float32\" }, { \"element_id\": 127, " +
                            "\"name\": \"sourceIpLongitude\", \"data_type\": \"float32\" }, { \"element_id\": 140, " +
                            "\"name\": \"destinationIpCountryCode\", \"data_type\": \"string\" }, { \"element_id\": 141, " +
                            "\"name\": \"destinationIpCountryName\", \"data_type\": \"string\" }, { \"element_id\": 142, " +
                            "\"name\": \"destinationIpRegionCode\", \"data_type\": \"string\" }, { \"element_id\": 143, " +
                            "\"name\": \"destinationIpRegionName\", \"data_type\": \"string\" }, { \"element_id\": 145, " +
                            "\"name\": \"destinationIpCityName\", \"data_type\": \"string\" }, { \"element_id\": 146, " +
                            "\"name\": \"destinationIpLatitude\", \"data_type\": \"float32\" }, { \"element_id\": 147, " +
                            "\"name\": \"destinationIpLongitude\", \"data_type\": \"float32\" }, { \"element_id\": 160, " +
                            "\"name\": \"osDeviceId\", \"data_type\": \"unsigned8\" }, { \"element_id\": 161, " +
                            "\"name\": \"osDeviceName\", \"data_type\": \"string\" }, { \"element_id\": 162, " +
                            "\"name\": \"browserId\", \"data_type\": \"unsigned8\" }, { \"element_id\": 163, " +
                            "\"name\": \"browserName\", \"data_type\": \"string\" }, { \"element_id\": 176, " +
                            "\"name\": \"reverseOctetDeltaCount\", \"data_type\": \"unsigned64\" }, { \"element_id\": 177, " +
                            "\"name\": \"reversePacketDeltaCount\", \"data_type\": \"unsigned64\" }, { \"element_id\": 178, " +
                            "\"name\": \"sslConnectionEncryptionType\", \"data_type\": \"string\" }, { \"element_id\": 179, " +
                            "\"name\": \"sslEncryptionCipherName\", \"data_type\": \"string\" }, { \"element_id\": 180, " +
                            "\"name\": \"sslEncryptionKeyLength\", \"data_type\": \"unsigned16\" }, { \"element_id\": 182, " +
                            "\"name\": \"userAgent\", \"data_type\": \"string\" }, { \"element_id\": 183, " +
                            "\"name\": \"hostName\", \"data_type\": \"string\" }, { \"element_id\": 184, " +
                            "\"name\": \"uri\", \"data_type\": \"string\" }, { \"element_id\": 185, " +
                            "\"name\": \"dnsText\", \"data_type\": \"string\" }, { \"element_id\": 186, " +
                            "\"name\": \"sourceAsName\", \"data_type\": \"string\" }, { \"element_id\": 187, " +
                            "\"name\": \"destinationAsName\", \"data_type\": \"string\" }, { \"element_id\": 188, " +
                            "\"name\": \"transactionLatency\", \"data_type\": \"unsigned32\" }, { \"element_id\": 189, " +
                            "\"name\": \"dnsQueryHostName\", \"data_type\": \"string\" }, { \"element_id\": 190, " +
                            "\"name\": \"dnsResponseHostName\", \"data_type\": \"string\" }, { \"element_id\": 191, " +
                            "\"name\": \"dnsClasses\", \"data_type\": \"string\" }, { \"element_id\": 192, " +
                            "\"name\": \"threatType\", \"data_type\": \"string\" }, { \"element_id\": 193, " +
                            "\"name\": \"threatIpv4\", \"data_type\": \"ipv4address\" }, { \"element_id\": 194, " +
                            "\"name\": \"threatIpv6\", \"data_type\": \"ipv6address\" }, { \"element_id\": 195, " +
                            "\"name\": \"httpSession\", \"data_type\": \"subtemplatelist\" }, { \"element_id\": 196, " +
                            "\"name\": \"requestTime\", \"data_type\": \"unsigned32\" }, { \"element_id\": 197, " +
                            "\"name\": \"dnsRecord\", \"data_type\": \"subtemplatelist\" }, { \"element_id\": 198, " +
                            "\"name\": \"dnsName\", \"data_type\": \"string\" }, { \"element_id\": 199, " +
                            "\"name\": \"dnsIpv4Address\", \"data_type\": \"ipv4address\" }, { \"element_id\": 200, " +
                            "\"name\": \"dnsIpv6Address\", \"data_type\": \"ipv6address\" }, { \"element_id\": 201, " +
                            "\"name\": \"sni\", \"data_type\": \"string\" }, { \"element_id\": 457, " +
                            "\"name\": \"httpStatusCode\", \"data_type\": \"unsigned16\" }, { \"element_id\": 459, " +
                            "\"name\": \"httpRequestMethod\", \"data_type\": \"string\" }, { \"element_id\": 462, " +
                            "\"name\": \"httpMessageVersion\", \"data_type\": \"string\" } ] }\n";
        // Create a temporary json file.
        Files.write(tempFile.toPath(), custDefStr.getBytes(StandardCharsets.UTF_8));

        // load the configMap
        final HashMap<String, Object> configMap = new HashMap<>();
        configMap.put(IpfixCodec.CK_IPFIX_DEFINITION_PATH, tempFile.getAbsolutePath());
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
}
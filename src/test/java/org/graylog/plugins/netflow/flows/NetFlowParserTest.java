package org.graylog.plugins.netflow.flows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.graylog.plugins.netflow.v9.NetFlowV9FieldTypeRegistry;
import org.graylog.plugins.netflow.v9.NetFlowV9TemplateCache;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class NetFlowParserTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();
    private NetFlowV9FieldTypeRegistry typeRegistry;
    private NetFlowV9TemplateCache templateCache;

    @Before
    public void setUp() throws Exception {
        typeRegistry = NetFlowV9FieldTypeRegistry.create();
        templateCache = new NetFlowV9TemplateCache(
                1000L,
                temporaryFolder.newFile().toPath(),
                30 * 60,
                Executors.newSingleThreadScheduledExecutor(),
                objectMapper);
    }

    @Test
    public void parseReturnsNullIfMessageWasInvalid() throws Exception {
        final byte[] b = "Foobar".getBytes(StandardCharsets.UTF_8);
        final InetSocketAddress source = new InetSocketAddress(InetAddress.getLocalHost(), 12345);
        final RawMessage rawMessage = new RawMessage(b, source);

        final List<Message> messages = NetFlowParser.parse(rawMessage, templateCache, typeRegistry);
        assertThat(messages).isNull();
    }

    @Test
    public void parsePropagatesFlowException() throws Exception {
        final byte[] b = "Foobar".getBytes(StandardCharsets.UTF_8);
        final InetSocketAddress source = new InetSocketAddress(InetAddress.getLocalHost(), 12345);
        final RawMessage rawMessage = new RawMessage(b, source) {
            @Override
            public byte[] getPayload() {
                throw new FlowException("Boom!");
            }
        };

        assertThatExceptionOfType(FlowException.class)
                .isThrownBy(() -> NetFlowParser.parse(rawMessage, templateCache, typeRegistry))
                .withMessage("Boom!");
    }

    @Test
    public void parseSuccessfullyDecodesNetFlowV5() throws Exception {
        final byte[] b = Resources.toByteArray(Resources.getResource("netflow-data/netflow-v5-1.dat"));
        final InetSocketAddress source = new InetSocketAddress(InetAddress.getLocalHost(), 12345);
        final RawMessage rawMessage = new RawMessage(b, source);

        final List<Message> messages = NetFlowParser.parse(rawMessage, templateCache, typeRegistry);
        assertThat(messages)
                .isNotNull()
                .hasSize(2);

        final Message message1 = messages.get(0);
        assertThat(message1).isNotNull();

        assertThat(message1.getMessage()).isEqualTo("NetFlowV5 [10.0.2.2]:54435 <> [10.0.2.15]:22 proto:6 pkts:5 bytes:230");
        assertThat(message1.getTimestamp()).isEqualTo(new DateTime(2015, 5, 2, 18, 38, 8, 280, DateTimeZone.UTC));
        assertThat(message1.getSource()).isEqualTo(source.getAddress().getHostAddress());
        assertThat(message1.getFields())
                .containsEntry("nf_version", 5)
                .containsEntry("nf_src_address", "10.0.2.2")
                .containsEntry("nf_src_port", 54435)
                .containsEntry("nf_dst_address", "10.0.2.15")
                .containsEntry("nf_dst_port", 22)
                .containsEntry("nf_proto", (short) 6)
                .containsEntry("nf_proto_name", "TCP")
                .containsEntry("nf_tcp_flags", (short) 16);
    }

    @Test
    public void parseSuccessfullyDecodesNetFlowV9() throws Exception {
        final byte[] b1 = Resources.toByteArray(Resources.getResource("netflow-data/netflow-v9-2-1.dat"));
        final byte[] b2 = Resources.toByteArray(Resources.getResource("netflow-data/netflow-v9-2-2.dat"));
        final byte[] b3 = Resources.toByteArray(Resources.getResource("netflow-data/netflow-v9-2-3.dat"));
        final InetSocketAddress source = new InetSocketAddress(InetAddress.getLocalHost(), 12345);

        final List<Message> messages1 = NetFlowParser.parse(new RawMessage(b1, source), templateCache, typeRegistry);
        assertThat(messages1).isEmpty();
        final List<Message> messages2 = NetFlowParser.parse(new RawMessage(b2, source), templateCache, typeRegistry);
        assertThat(messages2).isNotNull().hasSize(1);

        final Message message2 = messages2.get(0);
        assertThat(message2).isNotNull();
        assertThat(message2.getMessage()).isEqualTo("NetFlowV9 [192.168.124.1]:3072 <> [239.255.255.250]:1900 proto:17 pkts:8 bytes:2818");
        assertThat(message2.getTimestamp()).isEqualTo(DateTime.parse("2013-05-21T07:51:49.000Z"));
        assertThat(message2.getSource()).isEqualTo(source.getAddress().getHostAddress());
        assertThat(message2.getFields())
                .containsEntry("nf_src_address", "192.168.124.1")
                .containsEntry("nf_src_port", 3072)
                .containsEntry("nf_dst_address", "239.255.255.250")
                .containsEntry("nf_dst_port", 1900)
                .containsEntry("nf_proto", (short) 17)
                .containsEntry("nf_proto_name", "UDP");


        final List<Message> messages3 = NetFlowParser.parse(new RawMessage(b3, source), templateCache, typeRegistry);
        assertThat(messages3).isNotNull().hasSize(1);

        final Message message3 = messages3.get(0);
        assertThat(message3).isNotNull();
        assertThat(message3.getMessage()).isEqualTo("NetFlowV9 [192.168.124.20]:42444 <> [121.161.231.32]:9090 proto:17 pkts:2 bytes:348");
        assertThat(message3.getTimestamp()).isEqualTo(DateTime.parse("2013-05-21T07:52:43.000Z"));
        assertThat(message3.getSource()).isEqualTo(source.getAddress().getHostAddress());
        assertThat(message3.getFields())
                .containsEntry("nf_src_address", "192.168.124.20")
                .containsEntry("nf_src_port", 42444)
                .containsEntry("nf_dst_address", "121.161.231.32")
                .containsEntry("nf_dst_port", 9090)
                .containsEntry("nf_proto", (short) 17)
                .containsEntry("nf_proto_name", "UDP");
    }

}
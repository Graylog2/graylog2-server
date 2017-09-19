package org.graylog.plugins.cef.codec;

import org.graylog.plugins.cef.parser.CEFMessage;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.journal.RawMessage;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class BaseCEFCodecTest {
    private BaseCEFCodec codec;

    @Before
    public void setUp() {
        codec = new BaseCEFCodec(Configuration.EMPTY_CONFIGURATION) {
            @Override
            public String getName() {
                return "name";
            }

            @Nullable
            @Override
            public Message decode(@Nonnull RawMessage rawMessage) {
                return null;
            }
        };
    }

    @Test
    public void buildMessageSummary() throws Exception {
        final CEFMessage cefMessage = CEFMessage.builder()
                .version(0)
                .deviceVendor("vendor")
                .deviceProduct("product")
                .deviceVersion("1.0.0")
                .deviceEventClassId("event-class-id")
                .name("name")
                .severity(CEFMessage.Severity.parse("High"))
                .fields(Collections.emptyMap())
                .build();
        assertEquals("product: [event-class-id, High] name", codec.buildMessageSummary(cefMessage));
    }

    @Test
    public void decideSourceWithoutDeviceAddressReturnsRawMessageRemoteAddress() throws Exception {
        final CEFMessage cefMessage = CEFMessage.builder()
                .version(0)
                .deviceVendor("vendor")
                .deviceProduct("product")
                .deviceVersion("1.0.0")
                .deviceEventClassId("event-class-id")
                .name("name")
                .severity(CEFMessage.Severity.parse("High"))
                .fields(Collections.emptyMap())
                .build();
        final RawMessage rawMessage = new RawMessage(new byte[0], new InetSocketAddress("128.66.23.42", 12345));

        // The hostname is unresolved, so we have to add the leading slash. Oh, Java...
        assertEquals("/128.66.23.42", codec.decideSource(cefMessage, rawMessage));
    }

    @Test
    public void decideSourceWithFullDeviceAddressReturnsExtensionValue() throws Exception {
        final CEFMessage cefMessage = CEFMessage.builder()
                .version(0)
                .deviceVendor("vendor")
                .deviceProduct("product")
                .deviceVersion("1.0.0")
                .deviceEventClassId("event-class-id")
                .name("name")
                .severity(CEFMessage.Severity.parse("High"))
                .fields(Collections.singletonMap("deviceAddress", "128.66.23.42"))
                .build();
        final RawMessage rawMessage = new RawMessage(new byte[0], new InetSocketAddress("example.com", 12345));
        codec.decideSource(cefMessage, rawMessage);
        assertEquals("128.66.23.42", codec.decideSource(cefMessage, rawMessage));
    }

    @Test
    public void decideSourceWithShortDeviceAddressReturnsExtensionValue() throws Exception {
        final CEFMessage cefMessage = CEFMessage.builder()
                .version(0)
                .deviceVendor("vendor")
                .deviceProduct("product")
                .deviceVersion("1.0.0")
                .deviceEventClassId("event-class-id")
                .name("name")
                .severity(CEFMessage.Severity.parse("High"))
                .fields(Collections.singletonMap("dvc", "128.66.23.42"))
                .build();
        final RawMessage rawMessage = new RawMessage(new byte[0], new InetSocketAddress("example.com", 12345));
        codec.decideSource(cefMessage, rawMessage);
        assertEquals("128.66.23.42", codec.decideSource(cefMessage, rawMessage));
    }

    @Test
    public void getAggregator() throws Exception {
        assertNull(codec.getAggregator());
    }
}
package org.graylog2.plugin.inputs.transports;

import com.google.common.collect.ImmutableMap;
import com.google.common.eventbus.EventBus;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import org.graylog2.inputs.transports.NettyTransportConfiguration;
import org.graylog2.inputs.transports.UdpTransport;
import org.graylog2.inputs.transports.netty.EventLoopGroupFactory;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.util.ThroughputCounter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class FileTransportTest {

    private static final ImmutableMap<String, Object> CONFIG_SOURCE = ImmutableMap.of(
            FileTransport.custom_field_def_dest_file_path, "/config/ipfix/",
            FileTransport.custom_field_def_filename, "custom_field_ipfix.json"
          );

    private static final Configuration CONFIGURATION = new Configuration(CONFIG_SOURCE);

    //Do we use the same configuration for File Transport as well
    private final NettyTransportConfiguration nettyTransportConfiguration = new NettyTransportConfiguration("nio", "jdk", 1);
    private FileTransport fileTransport;
    private EventLoopGroup eventLoopGroup;
    private EventBus eventBus;


    @Before
    public void setUp() {
        eventLoopGroup = new NioEventLoopGroup();
        eventBus = new EventBus();

    }

    @After
    public void tearDown() {
         eventLoopGroup.shutdownGracefully();

    }

    @Test
    public void produceRawMessage() {
       fileTransport = new FileTransport(eventBus, CONFIGURATION);
       assertNull(fileTransport.produceRawMessage(null));
    }
}

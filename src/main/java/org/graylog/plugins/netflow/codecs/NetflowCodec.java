package org.graylog.plugins.netflow.codecs;

import com.google.common.collect.Lists;
import com.google.inject.assistedinject.Assisted;
import org.graylog.plugins.netflow.flows.FlowException;
import org.graylog.plugins.netflow.flows.NetFlowParser;
import org.graylog.plugins.netflow.flows.cflow.NetFlowV5;
import org.graylog.plugins.netflow.flows.cflow.NetFlowV5Packet;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.inputs.annotations.Codec;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.codecs.AbstractCodec;
import org.graylog2.plugin.journal.RawMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.List;

@Codec(name = "netflow", displayName = "Netflow")
public class NetflowCodec extends AbstractCodec {
    private static final Logger LOG = LoggerFactory.getLogger(NetflowCodec.class);

    @Inject
    protected NetflowCodec(@Assisted Configuration configuration) {
        super(configuration);
    }

    @Nullable
    @Override
    public Message decode(RawMessage rawMessage) {
        LOG.info("DECODE NETFLOW");

        try {
            final NetFlowV5Packet packet = NetFlowParser.parse(rawMessage);

            if (packet == null) {
                return null;
            }

            final List<Message> messages = Lists.newArrayList();

            for (NetFlowV5 flow : packet.flows) {
                final String source = rawMessage.getRemoteAddress() != null ? rawMessage.getRemoteAddress().getAddress().toString() : null;
                final Message message = new Message(flow.toMessageString(), source, flow.timestamp);

                messages.add(message);
                LOG.info("NetFLow Message: {}", message);
            }

            //LOG.info("NetFlow packet: count={} - {}", packet.getCount(), packet);
        } catch (FlowException e) {
            LOG.error("Error parsing NetFlow packet", e);
        }

        return null;
    }

    @FactoryClass
    public interface Factory extends AbstractCodec.Factory<NetflowCodec> {
        @Override
        NetflowCodec create(Configuration configuration);

        @Override
        Config getConfig();
    }

    @ConfigClass
    public static class Config extends AbstractCodec.Config {
        @Override
        public void overrideDefaultValues(@Nonnull ConfigurationRequest cr) {
        }
    }
}

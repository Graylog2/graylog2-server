package org.graylog2.outputs;

import org.joda.time.DateTime;
import org.graylog2.GelfMessage;
import org.graylog2.GelfSender;
import org.graylog2.GelfTCPSender;
import org.graylog2.GelfUDPSender;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.*;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.outputs.MessageOutputConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class GelfOutput implements MessageOutput {
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());
    private Configuration configuration;
    private GelfSender gelfSender;

    @Override
    public void initialize(Configuration config) throws MessageOutputConfigurationException {
        configuration = config;
        gelfSender = getGelfSender(configuration);
    }

    protected GelfSender getGelfSender(Configuration configuration) throws MessageOutputConfigurationException {
        final String hostname = configuration.getString("hostname");
        final int port = Integer.parseInt(configuration.getString("port"));

        LOG.debug("Connecting to {}:{}", hostname, port);

        final GelfSender gelfSender;
        try {
            if (configuration.getString("protocol").toUpperCase().equals("UDP")) {
                LOG.debug("Initializing UDP sender");
                gelfSender = new GelfUDPSender(hostname, port);
            } else {
                LOG.debug("Initializing TCP sender");
                gelfSender = new GelfTCPSender(hostname, port);
            }
        } catch (IOException e) {
            final String error = "Error initializing " + this.getClass() + ": " + e;
            LOG.error(error);
            throw new MessageOutputConfigurationException(error);
        }

        return gelfSender;
    }

    @Override
    public void write(Message message) throws Exception {
        if (gelfSender == null)
            gelfSender = getGelfSender(this.configuration);
        gelfSender.sendMessage(toGELFMessage(message));
    }

    protected GelfMessage toGELFMessage(Message message) {
        final DateTime curTimestamp;
        if (message.getField("timestamp") != null || message.getField("timestamp") instanceof DateTime)
            curTimestamp = (DateTime)message.getField("timestamp");
        else
            curTimestamp = DateTime.now();

        final long timestamp = curTimestamp.getMillis()/1000;
        GelfMessage gelfMessage = new GelfMessage((String)message.getField("short_message"),
                (String)message.getField("message"),
                timestamp,
                (String)message.getField("level"));

        gelfMessage.setHost((String)message.getField("source"));
        gelfMessage.setFacility(this.getClass().getCanonicalName());

        gelfMessage.setAdditonalFields(message.getFields());

        return gelfMessage;
    }

    @Override
    public ConfigurationRequest getRequestedConfiguration() {
        ConfigurationRequest configurationRequest = new ConfigurationRequest();
        configurationRequest.addField(new TextField("hostname", "Destination host", "", "This is the hostname of the destination", ConfigurationField.Optional.NOT_OPTIONAL));
        configurationRequest.addField(new NumberField("port", "Destination port", 12201, "This is the port of the destination", ConfigurationField.Optional.NOT_OPTIONAL));
        Map<String, String> protocols = new HashMap<String, String>() {{
            put("TCP", "TCP");
            put("UDP", "UDP");
        }};
        configurationRequest.addField(new DropdownField("protocol", "Protocol", "TCP", protocols, "The protocol used to connect", ConfigurationField.Optional.OPTIONAL));
        return configurationRequest;
    }

    @Override
    public String getName() {
        return "GELF Output";
    }

    @Override
    public String getHumanName() {
        return "An output sending GELF over TCP or UDP";
    }

    @Override
    public String getLinkToDocs() {
        return null;
    }
}

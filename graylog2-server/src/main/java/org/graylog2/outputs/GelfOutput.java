package org.graylog2.outputs;

import com.google.common.collect.Maps;
import org.graylog2.GelfMessage;
import org.graylog2.GelfSender;
import org.graylog2.GelfTCPSender;
import org.graylog2.GelfUDPSender;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.DropdownField;
import org.graylog2.plugin.configuration.fields.NumberField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.outputs.MessageOutputConfigurationException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class GelfOutput implements MessageOutput {
    private static final Logger LOG = LoggerFactory.getLogger(GelfOutput.class);
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private Configuration configuration;
    private GelfSender gelfSender;

    @Override
    public void initialize(Configuration config) throws MessageOutputConfigurationException {
        configuration = config;
        gelfSender = getGelfSender(configuration);
        isRunning.set(true);
    }

    @Override
    public void stop() {
        LOG.debug("Closing {}", gelfSender.getClass().getName());
        try {
            isRunning.set(false);
            gelfSender.close();
        } catch (Exception e) {
            LOG.error("Error closing " + gelfSender.getClass().getName(), e);
        }
    }

    @Override
    public boolean isRunning() {
        return isRunning.get();
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

    @Override
    public void write(List<Message> messages) throws Exception {
        for (Message message : messages) {
            write(message);
        }

    }

    protected GelfMessage toGELFMessage(final Message message) {
        final DateTime timestamp;
        if (message.getField("timestamp") != null || message.getField("timestamp") instanceof DateTime) {
            timestamp = (DateTime) message.getField("timestamp");
        } else {
            timestamp = DateTime.now();
        }

        final String level = (String) message.getField("level");
        final String messageLevel = level == null? "1" : level;
        final String fullMessage = (String) message.getField("full_message");
        final String facility = (String) message.getField("facility");

        final GelfMessage gelfMessage = new GelfMessage();

        gelfMessage.setShortMessage(message.getMessage());

        if(fullMessage != null) {
            gelfMessage.setFullMessage(fullMessage);
        }

        gelfMessage.setJavaTimestamp(timestamp.getMillis());
        gelfMessage.setLevel(messageLevel);
        gelfMessage.setHost(message.getSource());

        if(facility != null) {
            gelfMessage.setFacility(facility);
        }

        gelfMessage.setAdditonalFields(Maps.newHashMap(message.getFields()));

        final String forwarder = GelfOutput.class.getCanonicalName();
        gelfMessage.addField("_forwarder", forwarder);

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

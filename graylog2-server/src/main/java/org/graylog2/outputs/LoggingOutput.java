/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.outputs;

import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.outputs.MessageOutputConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class LoggingOutput implements MessageOutput {
    private static final Logger LOG = LoggerFactory.getLogger(LoggingOutput.class);
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private Configuration configuration;

    @Override
    public void initialize(Configuration config) throws MessageOutputConfigurationException {
        LOG.info("Initializing");
        configuration = config;
        isRunning.set(true);
    }

    @Override
    public void stop() {
        isRunning.set(false);
        LOG.info("Stopping");
    }

    @Override
    public boolean isRunning() {
        return isRunning.get();
    }

    @Override
    public void write(Message message) throws Exception {
        LOG.info("{} {}", configuration.getString("prefix"), message);
    }

    @Override
    public void write(List<Message> messages) throws Exception {
        for (Message message : messages) {
            write(message);
        }

    }

    @Override
    public ConfigurationRequest getRequestedConfiguration() {
        ConfigurationRequest configurationRequest = new ConfigurationRequest();
        configurationRequest.addField(new TextField("prefix", "Prefix", "Writing message: ", "How to prefix the message before logging it", ConfigurationField.Optional.OPTIONAL));
        return configurationRequest;
    }

    @Override
    public String getName() {
        return "STDOUT Output";
    }

    @Override
    public String getHumanName() {
        return "An output writing every message to STDOUT.";
    }

    @Override
    public String getLinkToDocs() {
        return null;
    }
}

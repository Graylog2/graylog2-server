/*
 * Copyright 2012-2014 TORCH GmbH
 *
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

import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class LoggingOutput implements MessageOutput {
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());
    private Configuration configuration;

    @Override
    public void initialize(Configuration config) throws MessageOutputConfigurationException {
        LOG.info("Initializing");
        this.configuration = configuration;
    }

    @Override
    public void write(Message message) throws Exception {
        LOG.info("Writing message {}", message);
    }

    @Override
    public ConfigurationRequest getRequestedConfiguration() {
        ConfigurationRequest configurationRequest = new ConfigurationRequest();
        configurationRequest.addField(new TextField("sample_field", "Sample Field", "A default value", "This ist just a sample field", ConfigurationField.Optional.OPTIONAL));
        return configurationRequest;
    }

    @Override
    public Map<String, String> getRequestedStreamConfiguration() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getName() {
        return "A logging only output.";
    }
}

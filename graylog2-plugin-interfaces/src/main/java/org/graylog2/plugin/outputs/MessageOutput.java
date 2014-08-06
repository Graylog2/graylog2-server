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

package org.graylog2.plugin.outputs;

import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;

import java.util.List;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public interface MessageOutput {

    public void initialize(Configuration config) throws MessageOutputConfigurationException;
    public void stop();
    public boolean isRunning();
    public void write(Message message) throws Exception;
    public void write(List<Message> messages) throws Exception;
    public ConfigurationRequest getRequestedConfiguration();
    public String getName();
    public String getHumanName();
    public String getLinkToDocs();
}

/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
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
 *
 */
package org.graylog2.inputs.random;

import org.graylog2.Core;
import org.graylog2.inputs.random.generators.FakeHttpMessageGenerator;
import org.graylog2.plugin.GraylogServer;
import org.graylog2.plugin.buffers.BufferOutOfCapacityException;
import org.graylog2.plugin.buffers.ProcessingDisabledException;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.MisfireException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class FakeHttpMessageInput extends MessageInput {

    private static final Logger LOG = LoggerFactory.getLogger(FakeHttpMessageInput.class);

    public static final String NAME = "Random HTTP message generator";

    protected GraylogServer graylogServer;
    protected Configuration config;

    private boolean stopRequested = false;
    private int sleepMs = 50;

    @Override
    public void configure(Configuration config, GraylogServer graylogServer) throws ConfigurationException {
        this.graylogServer = graylogServer;
        this.config = config;
    }

    @Override
    public void launch() throws MisfireException {
        FakeHttpMessageGenerator generator = new FakeHttpMessageGenerator();
        while(!stopRequested) {
            graylogServer.getProcessBuffer().insertCached(generator.generate(), this);

            try {
                Thread.sleep(sleepMs);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    @Override
    public void stop() {
        this.stopRequested = true;
    }

    @Override
    public ConfigurationRequest getRequestedConfiguration() {
        return new ConfigurationRequest();
    }

    @Override
    public boolean isExclusive() {
        return false;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String linkToDocs() {
        return "";
    }

    @Override
    public Map<String, Object> getAttributes() {
        return config.getSource();
    }
}

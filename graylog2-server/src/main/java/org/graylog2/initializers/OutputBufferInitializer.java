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

package org.graylog2.initializers;

import com.beust.jcommander.internal.Maps;
import org.graylog2.buffers.OutputBuffer;
import org.graylog2.inputs.BasicCache;
import org.graylog2.plugin.initializers.Initializer;
import org.graylog2.plugin.initializers.InitializerConfigurationException;

import javax.inject.Inject;
import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class OutputBufferInitializer implements Initializer {
    private final OutputBuffer.Factory outputBufferFactory;
    private final BasicCache outputCache;

    @Inject
    public OutputBufferInitializer(OutputBuffer.Factory outputBufferFactory) {
        this.outputBufferFactory = outputBufferFactory;
        this.outputCache = new BasicCache();
    }

    @Override
    public void initialize(Map<String, String> config) throws InitializerConfigurationException {
        OutputBuffer outputBuffer = outputBufferFactory.create(outputCache);
        outputBuffer.initialize();
    }

    @Override
    public Map<String, String> getRequestedConfiguration() {
        return Maps.newHashMap();
    }

    @Override
    public String getName() {
        return "OutputBufferInitializer";
    }

    @Override
    public boolean masterOnly() {
        return false;
    }
}

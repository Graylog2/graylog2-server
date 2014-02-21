/**
 * Copyright 2014 Lennart Koopmann <lennart@torch.sh>
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
package org.graylog2.initializers;

import com.google.common.collect.Maps;
import org.graylog2.Core;
import org.graylog2.periodical.IndexFailureWriterThread;
import org.graylog2.plugin.GraylogServer;
import org.graylog2.plugin.initializers.Initializer;
import org.graylog2.plugin.initializers.InitializerConfigurationException;

import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class IndexFailureWriterInitializer implements Initializer {

    private static final String NAME = "Index failure writer";

    @Override
    public void initialize(GraylogServer server, Map<String, String> config) throws InitializerConfigurationException {
        Thread t = new Thread(new IndexFailureWriterThread((Core) server));

        t.setDaemon(true);
        t.start();
    }

    @Override
    public Map<String, String> getRequestedConfiguration() {
        return Maps.newHashMap();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean masterOnly() {
        return false;
    }
}

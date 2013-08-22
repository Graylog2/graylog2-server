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
package org.graylog2.filters;

import com.codahale.metrics.Timer;
import org.graylog2.Core;
import org.graylog2.plugin.GraylogServer;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.filters.MessageFilter;
import org.graylog2.plugin.inputs.Extractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class ExtractorFilter implements MessageFilter {

    private static final Logger LOG = LoggerFactory.getLogger(ExtractorFilter.class);

    private static final String NAME = "Extractor";

    @Override
    public boolean filter(Message msg, GraylogServer server) {
        if (msg.getSourceInput() == null) {
            return false;
        }

        for (Extractor extractor : msg.getSourceInput().getExtractors().values()) {
            try {
                extractor.runExtractor(server, msg);
            } catch (Exception e) {
                extractor.incrementExceptions();
                LOG.error("Could not apply extractor.", e);
                continue;
            }
        }

        return false;
    }

    @Override
    public String getName() {
        return NAME;
    }

}

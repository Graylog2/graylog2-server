/**
 * Copyright 2012 Lennart Koopmann <lennart@socketfeed.com>
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

package org.graylog2.outputs;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import org.graylog2.Core;
import org.graylog2.plugin.GraylogServer;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.outputs.MessageOutputConfigurationException;
import org.graylog2.plugin.outputs.OutputStreamConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class ElasticSearchOutput implements MessageOutput {

    private final Meter writes;
    private final Timer processTime;

    private static final String NAME = "ElasticSearch Output";
    
    private static final Logger LOG = LoggerFactory.getLogger(ElasticSearchOutput.class);

    public ElasticSearchOutput(Core core) {
        // Only constructing metrics here. write() get's another Core reference. (because this technically is a plugin)
        this.writes = core.metrics().meter(name(ElasticSearchOutput.class, "writes"));
        this.processTime = core.metrics().timer(name(ElasticSearchOutput.class, "processTime"));
    }

    @Override
    public void write(List<Message> messages, OutputStreamConfiguration streamConfig, GraylogServer server) throws Exception {
        LOG.debug("Writing <{}> messages.", messages.size());
        if (LOG.isTraceEnabled()) {
            final List<String> sortedIds = Ordering.natural().sortedCopy(Lists.transform(messages,  Message.ID_FUNCTION));
            LOG.trace("Writing message ids to [{}]: <{}>", getName(), Joiner.on(", ").join(sortedIds));
        }
        Core serverImpl = (Core) server;
        
        writes.mark();

        Timer.Context tcx = processTime.time();
        serverImpl.getIndexer().bulkIndex(messages);
        tcx.stop();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void initialize(Map<String, String> config) throws MessageOutputConfigurationException {
        // Built in output. This is just for plugin compat. Nothing to initialize.
    }

    @Override
    public Map<String, String> getRequestedConfiguration() {
        // Built in output. This is just for plugin compat. No special configuration required.
        return Maps.newHashMap();
    }
    
    @Override
    public Map<String, String> getRequestedStreamConfiguration() {
        // Built in output. This is just for plugin compat. No special configuration required.
        return Maps.newHashMap();
    }

}

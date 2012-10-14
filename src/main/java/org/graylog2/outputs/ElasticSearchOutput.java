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

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Meter;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;
import org.graylog2.Core;
import org.graylog2.plugin.logmessage.LogMessage;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class ElasticSearchOutput implements MessageOutput {

    private final Meter writes = Metrics.newMeter(ElasticSearchOutput.class, "Writes", "messages", TimeUnit.SECONDS);
    private final Timer processTime = Metrics.newTimer(ElasticSearchOutput.class, "ProcessTimeMilliseconds", TimeUnit.MILLISECONDS, TimeUnit.SECONDS);

    @Override
    public void write(List<LogMessage> messages, Core server) throws Exception {
        writes.mark();

        TimerContext tcx = processTime.time();
        server.getIndexer().bulkIndex(messages);
        tcx.stop();
    }



}

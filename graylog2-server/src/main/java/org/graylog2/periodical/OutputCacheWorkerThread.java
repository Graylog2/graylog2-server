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
package org.graylog2.periodical;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.Inject;
import org.graylog2.buffers.OutputBuffer;
import org.graylog2.inputs.OutputCache;
import org.graylog2.plugin.BaseConfiguration;
import org.graylog2.plugin.ServerStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class OutputCacheWorkerThread extends AbstractCacheWorkerThread {
    private static final Logger LOG = LoggerFactory.getLogger(OutputCacheWorkerThread.class);

    private final MetricRegistry metricRegistry;
    private final OutputCache outputCache;
    private final OutputBuffer outputBuffer;

    @Inject
    public OutputCacheWorkerThread(MetricRegistry metricRegistry,
                                   OutputCache outputCache,
                                   OutputBuffer outputBuffer,
                                   ServerStatus serverStatus,
                                   BaseConfiguration configuration) {
        super(serverStatus, configuration);
        this.metricRegistry = metricRegistry;
        this.outputCache = outputCache;
        this.outputBuffer = outputBuffer;
    }

    @Override
    public void doRun() {
        writtenMessages = metricRegistry.meter(name(OutputCacheWorkerThread.class, "writtenMessages"));
        outOfCapacity =  metricRegistry.meter(name(OutputCacheWorkerThread.class, "FailedWritesOutOfCapacity"));

        new Thread(new Runnable() {
            @Override
            public void run() {
                work(outputCache, outputBuffer);
            }
        }, "master-cache-worker-output").start();
    }}

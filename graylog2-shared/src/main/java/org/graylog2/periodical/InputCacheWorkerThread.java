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
import org.graylog2.inputs.InputCache;
import org.graylog2.plugin.BaseConfiguration;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.shared.buffers.ProcessBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class InputCacheWorkerThread extends AbstractCacheWorkerThread {
    private static final Logger LOG = LoggerFactory.getLogger(InputCacheWorkerThread.class);

    private final MetricRegistry metricRegistry;
    private final InputCache inputCache;
    private final ProcessBuffer processBuffer;

    @Inject
    public InputCacheWorkerThread(MetricRegistry metricRegistry,
                                  InputCache inputCache,
                                  ProcessBuffer processBuffer,
                                  ServerStatus serverStatus,
                                  BaseConfiguration configuration) {
        super(serverStatus, configuration);
        this.metricRegistry = metricRegistry;
        this.inputCache = inputCache;
        this.processBuffer = processBuffer;
    }

    @Override
    public void doRun() {
        writtenMessages = metricRegistry.meter(name(InputCacheWorkerThread.class, "writtenMessages"));
        outOfCapacity =  metricRegistry.meter(name(InputCacheWorkerThread.class, "FailedWritesOutOfCapacity"));

        new Thread(new Runnable() {
            @Override
            public void run() {
                work(inputCache, processBuffer);
            }
        }, "master-cache-worker-input").start();
    }

    @Override
    public int getParallelism() {
        return 1;
    }
}

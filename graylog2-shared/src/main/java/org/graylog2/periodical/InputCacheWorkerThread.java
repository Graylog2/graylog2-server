/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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

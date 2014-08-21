package org.graylog2.periodical;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.Inject;
import org.graylog2.buffers.OutputBuffer;
import org.graylog2.inputs.OutputCache;
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
                                   ServerStatus serverStatus) {
        super(serverStatus);
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

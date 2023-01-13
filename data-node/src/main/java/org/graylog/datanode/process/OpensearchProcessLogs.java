package org.graylog.datanode.process;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class OpensearchProcessLogs implements Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(OpensearchProcessLogs.class);

    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final long pid;
    private final CircularFifoQueue<String> stdOut;
    private final CircularFifoQueue<String> stdErr;

    public OpensearchProcessLogs(long pid, InputStream stdout, InputStream stderr, int logsBufferSize) {
        this.pid = pid;

        this.stdOut = new CircularFifoQueue<>(logsBufferSize);
        this.stdErr = new CircularFifoQueue<>(logsBufferSize);

        StreamConsumer outputConsumer = new StreamConsumer(stdout, line -> {
            stdOut.offer(line);
            LOG.info(line);
        });
        StreamConsumer errorConsumer = new StreamConsumer(stderr, line ->  {
            stdErr.offer(line);
            LOG.warn(line);
        });

        executor.submit(outputConsumer);
        executor.submit(errorConsumer);
    }

    public static OpensearchProcessLogs createFor(Process process, int logsBufferSize) {
        return new OpensearchProcessLogs(process.pid(), process.getInputStream(), process.getErrorStream(), logsBufferSize);
    }

    public List<String> getStdOut() {
        return new ArrayList<>(stdOut);
    }


    public List<String> getStdErr() {
        return new ArrayList<>(stdErr);
    }


    @Override
    public void close() {
        try {
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}

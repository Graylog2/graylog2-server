package org.graylog2.benchmarks.pipeline.singlebuffer;

import com.google.common.util.concurrent.AbstractFuture;

public class ProcessedMessage extends AbstractFuture<Boolean> {

    public void setProcessed() {
        set(Boolean.TRUE);
    }
}

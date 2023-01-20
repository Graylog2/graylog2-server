package org.graylog.datanode;

import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.ExecuteException;
import org.graylog.datanode.process.OpensearchProcess;
import org.graylog.datanode.process.ProcessEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventResultHandler extends DefaultExecuteResultHandler {

    private static final Logger LOG = LoggerFactory.getLogger(DataNodeRunner.class);

    private final OpensearchProcess process;

    public EventResultHandler(OpensearchProcess process) {
        this.process = process;
    }

    @Override
    public void onProcessComplete(int exitValue) {
        LOG.warn("Opensearch process terminated with exit value" + exitValue);
        process.onEvent(ProcessEvent.PROCESS_TERMINATED);
    }

    @Override
    public void onProcessFailed(ExecuteException e) {
        LOG.warn("Opensearch process failed", e);
        process.onEvent(ProcessEvent.PROCESS_TERMINATED);
    }
}

/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.datanode.management;

import com.github.oxo42.stateless4j.StateMachine;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.commons.exec.ExecuteException;
import org.graylog.datanode.configuration.DatanodeConfiguration;
import org.graylog.datanode.process.OpensearchConfiguration;
import org.graylog.datanode.process.ProcessEvent;
import org.graylog.datanode.process.OpensearchInfo;
import org.graylog.datanode.process.ProcessInformation;
import org.graylog.datanode.process.ProcessState;
import org.graylog.datanode.process.ProcessStateMachine;
import org.graylog.datanode.process.StateMachineTracer;
import org.graylog.shaded.opensearch2.org.apache.http.HttpHost;
import org.graylog.shaded.opensearch2.org.opensearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Queue;

class OpensearchProcessImpl implements OpensearchProcess, ProcessListener {

    private static final Logger LOG = LoggerFactory.getLogger(OpensearchProcessImpl.class);

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private Optional<OpensearchConfiguration> configuration = Optional.empty();
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private Optional<RestHighLevelClient> restClient = Optional.empty();

    private final StateMachine<ProcessState, ProcessEvent> processState;

    private final DatanodeConfiguration datanodeConfiguration;

    private boolean isLeaderNode;
    private OpensearchCommandLineProcess commandLineProcess;

    private final Queue<String> stdout;
    private final Queue<String> stderr;

    OpensearchProcessImpl(DatanodeConfiguration datanodeConfiguration, int logsCacheSize) {
        this.datanodeConfiguration = datanodeConfiguration;
        this.processState = ProcessStateMachine.createNew();
        this.stdout = new CircularFifoQueue<>(logsCacheSize);
        this.stderr = new CircularFifoQueue<>(logsCacheSize);
    }

    private RestHighLevelClient createRestClient(OpensearchConfiguration configuration) {
        return OpensearchRestClient.build(configuration);
    }

    @Override
    public List<String> stdOutLogs() {
        return stdout.stream().toList();
    }

    @Override
    public List<String> stdErrLogs() {
        return stderr.stream().toList();
    }

    public Optional<RestHighLevelClient> restClient() {
        return restClient;
    }

    public OpensearchInfo processInfo() {
        return new OpensearchInfo(datanodeConfiguration.nodeName(), processState.getState(), isLeaderNode, getOpensearchBaseUrl().toString(), commandLineProcess.processInfo());
    }

    @Override
    public URI getOpensearchBaseUrl() {
        final String baseUrl = configuration.map(OpensearchConfiguration::getRestBaseUrl)
                .map(HttpHost::toURI)
                .orElse(""); // TODO: will this cause problems in the nodeservice?
        return URI.create(baseUrl);
    }

    public void onEvent(ProcessEvent event) {
        LOG.debug("Process event: " + event);
        this.processState.fire(event);
    }

    @Override
    public void setStateMachineTracer(StateMachineTracer stateMachineTracer) {
        this.processState.setTrace(stateMachineTracer);
    }

    public void setLeaderNode(boolean isLeaderNode) {
        this.isLeaderNode = isLeaderNode;
    }

    @Override
    public boolean isLeaderNode() {
        return isLeaderNode;
    }

    public boolean isInState(ProcessState expectedState) {
        return this.processState.getState().equals(expectedState);
    }

    @Override
    public void startWithConfig(OpensearchConfiguration configuration) {
        this.configuration = Optional.of(configuration);
        restart();
        this.restClient = Optional.of(createRestClient(configuration));
    }

    @Override
    public synchronized void restart() {
        configuration.ifPresentOrElse(
                (config -> {
                    stopProcess();
                    commandLineProcess = new OpensearchCommandLineProcess(config, this);
                    commandLineProcess.start();
                }),
                () -> {throw new IllegalArgumentException("Opensearch configuration required but not supplied!");}
        );
    }

    @Override
    public synchronized void stop() {
        onEvent(ProcessEvent.PROCESS_STOPPED);
        stopProcess();
        stopRestClient();
    }

    private void stopRestClient() {
        restClient().ifPresent(client -> {
            try {
                client.close();
            } catch (IOException e) {
                LOG.warn("Failed to close rest client", e);
            }
        });
    }

    private void stopProcess() {
        if (this.commandLineProcess != null) {
            commandLineProcess.close();
        }
    }

    @Override
    public void onStart() {
        onEvent(ProcessEvent.PROCESS_STARTED);
    }

    @Override
    public void onStdOut(String line) {
        LOG.info(line);
        stdout.offer(line);
    }

    @Override
    public void onStdErr(String line) {
        LOG.warn(line);
        stderr.offer(line);
    }

    @Override
    public void onProcessComplete(int exitValue) {
        LOG.info("Opensearch process completed with exit code {}", exitValue);
        onEvent(ProcessEvent.PROCESS_TERMINATED);
    }

    @Override
    public void onProcessFailed(ExecuteException e) {
        LOG.warn("Opensearch process failed", e);
        onEvent(ProcessEvent.PROCESS_TERMINATED);
    }
}

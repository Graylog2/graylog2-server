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
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.ExecuteResultHandler;
import org.apache.http.HttpHost;
import org.graylog.datanode.EventResultHandler;
import org.graylog.datanode.ProcessProvidingExecutor;
import org.graylog.datanode.process.ExecOpensearchProcessLogs;
import org.graylog.datanode.process.OpensearchConfiguration;
import org.graylog.datanode.process.ProcessEvent;
import org.graylog.datanode.process.ProcessInfo;
import org.graylog.datanode.process.ProcessLogs;
import org.graylog.datanode.process.ProcessState;
import org.graylog.datanode.process.ProcessStateMachine;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

class OpensearchProcessImpl implements OpensearchProcess {

    private static final Logger LOG = LoggerFactory.getLogger(OpensearchProcessService.class);

    private final String opensearchVersion;
    private final Path targetLocation;
    private final OpensearchConfiguration configuration;
    private final int logsSize;
    private Process process;
    private ExecOpensearchProcessLogs processLogs;
    private final RestHighLevelClient restClient;

    private final StateMachine<ProcessState, ProcessEvent> processState;
    private final int httpPort;
    private final String nodeName;
    private boolean isLeaderNode;

    public OpensearchProcessImpl(OpensearchConfiguration configuration, int logsSize) {
        this.opensearchVersion = configuration.opensearchVersion();
        this.targetLocation = configuration.opensearchDir();
        this.httpPort = configuration.httpPort();
        this.nodeName = configuration.clusterConfiguration().nodeName();
        this.configuration = configuration;
        this.logsSize = logsSize;

        RestClientBuilder builder = RestClient.builder(new HttpHost("localhost", httpPort, "http"));
        this.restClient = new RestHighLevelClient(builder);
        this.processState = ProcessStateMachine.createNew();
    }

    public String opensearchVersion() {
        return opensearchVersion;
    }

    public ExecOpensearchProcessLogs getProcessLogs() {
        return processLogs;
    }

    public RestHighLevelClient restClient() {
        return restClient;
    }

    public ProcessState getStatus() {
        return processState.getState();
    }

    public ProcessInfo processInfo() {
        return new ProcessInfo(
                process.pid(),
                nodeName, processState.getState(),
                isLeaderNode,
                process.info().startInstant().orElse(null),
                process.info().totalCpuDuration().orElse(null),
                process.info().user().orElse(null),
                httpPort
        );
    }

    public void onEvent(ProcessEvent event) {
        this.processState.fire(event);
    }

    public void bind(Process process, ExecOpensearchProcessLogs logs) {
        this.process = process;
        this.processLogs = logs;
        onEvent(ProcessEvent.PROCESS_STARTED);
    }

    public void terminate() {
        this.process.destroy();
    }

    public void setLeaderNode(boolean isLeaderNode) {
        this.isLeaderNode = isLeaderNode;
    }

    public boolean isLeaderNode() {
        return isLeaderNode;
    }

    public String nodeName() {
        return nodeName;
    }

    public boolean isInState(ProcessState expectedState) {
        return this.getStatus().equals(expectedState);
    }

    @Override
    public void start() throws IOException {

        final Path binPath = this.configuration.opensearchDir().resolve(Paths.get("bin", "opensearch"));
        LOG.info("Running opensearch from " + binPath.toAbsolutePath());

        CommandLine cmdLine = new CommandLine(binPath.toAbsolutePath().toString());

        toConfigOptions(configuration.mergedConfig())
                .forEach(it -> cmdLine.addArgument(it, true));

        ProcessProvidingExecutor executor = new ProcessProvidingExecutor();
        final ExecOpensearchProcessLogs logger = new ExecOpensearchProcessLogs(this.logsSize);
        ExecuteResultHandler resultHandler = new EventResultHandler(this);
        executor.setStreamHandler(logger);
        executor.execute(cmdLine, resultHandler);

        final Process process;
        try {
            process = executor.getProcess().get(5, TimeUnit.SECONDS);
            bind(process, logger);
        } catch (TimeoutException e) {
            throw new RuntimeException("Failed to obtain process", e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ProcessLogs processLogs() {
        return this.processLogs;
    }

    @Override
    public void stop() {

    }

    private Stream<String> toConfigOptions(Map<String, String> mergedConfig) {
        return mergedConfig.entrySet().stream()
                .map(it -> String.format(Locale.ROOT, "-E%s=%s", it.getKey(), it.getValue()));
    }
}


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
package org.graylog.datanode.process;

import com.github.oxo42.stateless4j.StateMachine;
import org.apache.http.HttpHost;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.client.RestHighLevelClient;

import java.nio.file.Path;

public class OpensearchProcess {
    private final String opensearchVersion;
    private final Path targetLocation;
    private Process process;
    private final ExecOpensearchProcessLogs processLogs;
    private final RestHighLevelClient restClient;

    private final StateMachine<ProcessState, ProcessEvent> processState;
    private final int httpPort;
    private String nodeName;
    private boolean isLeaderNode;

    public OpensearchProcess(String opensearchVersion, Path targetLocation, ExecOpensearchProcessLogs processLogs, int httpPort, String nodeName) {
        this.opensearchVersion = opensearchVersion;
        this.targetLocation = targetLocation;
        this.processLogs = processLogs;
        this.httpPort = httpPort;
        this.nodeName = nodeName;

        RestClientBuilder builder = RestClient.builder(new HttpHost("localhost", httpPort, "http"));
        this.restClient = new RestHighLevelClient(builder);
        this.processState = ProcessStateMachine.createNew();
    }

    public String getOpensearchVersion() {
        return opensearchVersion;
    }

    public Path getTargetLocation() {
        return targetLocation;
    }

    public ExecOpensearchProcessLogs getProcessLogs() {
        return processLogs;
    }

    public RestHighLevelClient getRestClient() {
        return restClient;
    }

    public ProcessState getStatus() {
        return processState.getState();
    }

    public ProcessInfo getProcessInfo() {
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

    public void bind(Process process) {
        this.process = process;
        onEvent(ProcessEvent.PROCESS_STARTED);
    }

    public void setLeaderNode(boolean isLeaderNode) {
        this.isLeaderNode = isLeaderNode;
    }

    public boolean isLeaderNode() {
        return isLeaderNode;
    }

    public String getNodeName() {
        return nodeName;
    }

    public boolean hasPid(int processId) {
        return process != null && process.pid() == processId;
    }
}


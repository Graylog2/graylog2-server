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
import org.apache.http.HttpHost;
import org.graylog.datanode.process.OpensearchConfiguration;
import org.graylog.datanode.process.ProcessEvent;
import org.graylog.datanode.process.ProcessInfo;
import org.graylog.datanode.process.ProcessState;
import org.graylog.datanode.process.ProcessStateMachine;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

class OpensearchProcessImpl extends AbstractCommandLineProcess {

    private static final Logger LOG = LoggerFactory.getLogger(OpensearchProcessService.class);
    private final OpensearchConfiguration configuration;
    private int logsSize;

    private final RestHighLevelClient restClient;

    private final StateMachine<ProcessState, ProcessEvent> processState;
    private boolean isLeaderNode;

    public OpensearchProcessImpl(OpensearchConfiguration configuration, int logsSize) {
        this.configuration = configuration;
        this.logsSize = logsSize;
        RestClientBuilder builder = RestClient.builder(new HttpHost("localhost", configuration.httpPort(), "http"));
        this.restClient = new RestHighLevelClient(builder);
        this.processState = ProcessStateMachine.createNew();
    }

    public String opensearchVersion() {
        return configuration.opensearchVersion();
    }

    public RestHighLevelClient restClient() {
        return restClient;
    }

    public ProcessState getStatus() {
        return processState.getState();
    }

    public ProcessInfo processInfo() {

        return process().map(process -> new ProcessInfo(
                process.pid(),
                configuration.nodeName(), processState.getState(),
                isLeaderNode,
                process.info().startInstant().orElse(null),
                process.info().totalCpuDuration().orElse(null),
                process.info().user().orElse(null),
                configuration.httpPort()
        )).orElseGet(() -> new ProcessInfo(-1, configuration.nodeName(), processState.getState(), false, null, null, null, configuration.httpPort()));
    }

    public void onEvent(ProcessEvent event) {
        this.processState.fire(event);
    }

    public void setLeaderNode(boolean isLeaderNode) {
        this.isLeaderNode = isLeaderNode;
    }

    public boolean isLeaderNode() {
        return isLeaderNode;
    }

    public String nodeName() {
        return configuration.nodeName();
    }

    public boolean isInState(ProcessState expectedState) {
        return this.getStatus().equals(expectedState);
    }

    @Override
    protected Path getExecutable() {
        return this.configuration.opensearchDir().resolve(Paths.get("bin", "opensearch"));
    }

    @Override
    protected List<String> getCommandLineArguments() {
        return configuration.asMap().entrySet().stream()
                .map(it -> String.format(Locale.ROOT, "-E%s=%s", it.getKey(), it.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    protected int getLogsSize() {
        return logsSize;
    }
}


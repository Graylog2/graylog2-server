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
import org.apache.http.client.utils.URIBuilder;
import org.graylog.datanode.Configuration;
import org.graylog.datanode.configuration.DatanodeConfiguration;
import org.graylog.datanode.process.OpensearchConfiguration;
import org.graylog.datanode.process.OpensearchInfo;
import org.graylog.datanode.process.ProcessEvent;
import org.graylog.datanode.process.ProcessInformation;
import org.graylog.datanode.process.ProcessState;
import org.graylog.datanode.process.ProcessStateMachine;
import org.graylog.datanode.process.StateMachineTracer;
import org.graylog.shaded.opensearch2.org.opensearch.client.RestHighLevelClient;
import org.graylog2.cluster.Node;
import org.graylog2.cluster.NodeService;
import org.graylog2.security.CustomCAX509TrustManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

class OpensearchProcessImpl implements OpensearchProcess, ProcessListener {

    private static final Logger LOG = LoggerFactory.getLogger(OpensearchProcessImpl.class);
    public static final Path UNICAST_HOSTS_FILE = Path.of("unicast_hosts.txt");
    private final StateMachineTracerAggregator tracerAggregator;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private Optional<OpensearchConfiguration> opensearchConfiguration = Optional.empty();
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private Optional<RestHighLevelClient> restClient = Optional.empty();

    private final StateMachine<ProcessState, ProcessEvent> processState;

    private final DatanodeConfiguration datanodeConfiguration;

    private boolean isLeaderNode;
    private OpensearchCommandLineProcess commandLineProcess;

    private final Queue<String> stdout;
    private final Queue<String> stderr;
    private final CustomCAX509TrustManager trustManager;
    private final NodeService nodeService;
    private final Configuration configuration;


    OpensearchProcessImpl(DatanodeConfiguration datanodeConfiguration, int logsCacheSize, final CustomCAX509TrustManager trustManager, final Configuration configuration, final NodeService nodeService) {
        this.datanodeConfiguration = datanodeConfiguration;
        this.processState = ProcessStateMachine.createNew();
        tracerAggregator = new StateMachineTracerAggregator();
        this.processState.setTrace(tracerAggregator);
        this.stdout = new CircularFifoQueue<>(logsCacheSize);
        this.stderr = new CircularFifoQueue<>(logsCacheSize);
        this.trustManager = trustManager;
        this.nodeService = nodeService;
        this.configuration = configuration;
    }

    private RestHighLevelClient createRestClient(OpensearchConfiguration configuration) {
        return OpensearchRestClient.build(configuration, datanodeConfiguration, trustManager);
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
        return new OpensearchInfo(configuration.getDatanodeNodeName(), processState.getState(), isLeaderNode, getOpensearchBaseUrl().toString(), commandLineProcess != null ? commandLineProcess.processInfo() : ProcessInformation.empty());
    }

    @Override
    public URI getOpensearchBaseUrl() {
        final String baseUrl = opensearchConfiguration.map(OpensearchConfiguration::getRestBaseUrl)
                .map(httpHost -> new URIBuilder()
                        .setHost(httpHost.getHostName())
                        .setPort(httpHost.getPort())
                        .setScheme(httpHost.getSchemeName()).toString())
                .orElse(""); // TODO: will this cause problems in the nodeservice?
        return URI.create(baseUrl);
    }

    @Override
    public String getOpensearchClusterUrl() {
        return configuration.getDatanodeNodeName() + ":" + configuration.getOpensearchTransportPort();
    }

    public void onEvent(ProcessEvent event) {
        LOG.debug("Process event: " + event);
        this.processState.fire(event);
    }

    @Override
    public void addStateMachineTracer(StateMachineTracer stateMachineTracer) {
        this.tracerAggregator.addTracer(stateMachineTracer);
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
        this.opensearchConfiguration = Optional.of(configuration);
        restart();
    }

    private void writeSeedHostsList() {
        try {
            final Path hostsfile = datanodeConfiguration.datanodeDirectories().createOpensearchProcessConfigurationFile(UNICAST_HOSTS_FILE);
            final Set<String> current = nodeService.allActive(Node.Type.DATANODE).values().stream().map(Node::getClusterAddress).filter(Objects::nonNull).collect(Collectors.toSet());
            Files.write(hostsfile, current, Charset.defaultCharset(), StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        } catch (IOException iox) {
            LOG.error("Could not write to file: {} - {}", UNICAST_HOSTS_FILE, iox.getMessage());
        }

    }
    @Override
    public synchronized void restart() {
        opensearchConfiguration.ifPresentOrElse(
                (config -> {
                    stopProcess();
                    // refresh TM if the SSL certs changed
                    trustManager.refresh();
                    // refresh the seed hosts
                    writeSeedHostsList();
                    commandLineProcess = new OpensearchCommandLineProcess(config, this);
                    commandLineProcess.start();
                    restClient = Optional.of(createRestClient(config));
                }),
                () -> {throw new IllegalArgumentException("Opensearch configuration required but not supplied!");}
        );
    }

    @Override
    public synchronized void stop() {
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
            onEvent(ProcessEvent.PROCESS_STOPPED);
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

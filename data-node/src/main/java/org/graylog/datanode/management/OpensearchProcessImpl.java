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
import org.graylog.datanode.process.OpensearchConfiguration;
import org.graylog.datanode.process.ProcessEvent;
import org.graylog.datanode.process.ProcessInfo;
import org.graylog.datanode.process.ProcessState;
import org.graylog.datanode.process.ProcessStateMachine;
import org.graylog.shaded.opensearch2.org.apache.http.HttpHost;
import org.graylog.shaded.opensearch2.org.apache.http.auth.AuthScope;
import org.graylog.shaded.opensearch2.org.apache.http.auth.UsernamePasswordCredentials;
import org.graylog.shaded.opensearch2.org.apache.http.client.CredentialsProvider;
import org.graylog.shaded.opensearch2.org.apache.http.impl.client.BasicCredentialsProvider;
import org.graylog.shaded.opensearch2.org.opensearch.client.RestClient;
import org.graylog.shaded.opensearch2.org.opensearch.client.RestClientBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Queue;

class OpensearchProcessImpl implements OpensearchProcess, ProcessListener {

    private static final Logger LOG = LoggerFactory.getLogger(OpensearchProcessImpl.class);
    private final OpensearchConfiguration configuration;
    private final RestHighLevelClient restClient;

    private final StateMachine<ProcessState, ProcessEvent> processState;
    private boolean isLeaderNode;
    private CommandLineProcess commandLineProcess;

    private final Queue<String> stdout;
    private final Queue<String> stderr;

    OpensearchProcessImpl(OpensearchConfiguration configuration, int logsCacheSize) {
        this.configuration = configuration;
        this.restClient = createRestClient(configuration);
        this.processState = ProcessStateMachine.createNew();
        this.stdout = new CircularFifoQueue<>(logsCacheSize);
        this.stderr = new CircularFifoQueue<>(logsCacheSize);
    }

    private RestHighLevelClient createRestClient(OpensearchConfiguration configuration) {
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

        final HttpHost host = getRestBaseUrl(configuration);

        RestClientBuilder builder = RestClient.builder(host);
        if ("https".equals(host.getSchemeName())) {
            if (configuration.authUsername() != null && configuration.authPassword() != null) {
                credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(configuration.authUsername(), configuration.authPassword()));
            }
            builder.setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
        }
        return new RestHighLevelClient(builder);
    }

    public String opensearchVersion() {
        return configuration.opensearchVersion();
    }

    @Override
    public List<String> stdOutLogs() {
        return stdout.stream().toList();
    }

    @Override
    public List<String> stdErrLogs() {
        return stderr.stream().toList();
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
                getRestBaseUrl(configuration).toString()

        )).orElseGet(() -> new ProcessInfo(-1, configuration.nodeName(), processState.getState(), false, null, null, null, null));
    }

    @Override
    public URI getOpensearchBaseUrl() {
        return URI.create(getRestBaseUrl(configuration).toURI());
    }

    private HttpHost getRestBaseUrl(OpensearchConfiguration config) {
        final boolean sslEnabled = Boolean.parseBoolean(config.asMap().getOrDefault("plugins.security.ssl.http.enabled", "false"));
        return new HttpHost("localhost", config.httpPort(), sslEnabled ? "https" : "http");
    }

    private Optional<Process> process() {
        return Optional.ofNullable(commandLineProcess).flatMap(CommandLineProcess::getProcess);
    }

    public void onEvent(ProcessEvent event) {
        this.processState.fire(event);
    }

    public void setLeaderNode(boolean isLeaderNode) {
        this.isLeaderNode = isLeaderNode;
    }

    @Override
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
    public synchronized void start() throws IOException {
        final Path executable = configuration.opensearchDir().resolve(Paths.get("bin", "opensearch"));
        final List<String> arguments = configuration.asMap().entrySet().stream()
                .map(it -> String.format(Locale.ROOT, "-E%s=%s", it.getKey(), it.getValue())).toList();
        commandLineProcess = new CommandLineProcess(executable, arguments, this);
        commandLineProcess.start();
    }

    @Override
    public synchronized void stop() {
        commandLineProcess.stop();
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


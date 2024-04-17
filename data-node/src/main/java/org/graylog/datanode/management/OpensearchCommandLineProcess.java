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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.RetryListener;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.exec.OS;
import org.graylog.datanode.management.opensearch.cli.OpensearchCli;
import org.graylog.datanode.state.OpensearchConfiguration;
import org.graylog.datanode.state.ProcessInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class OpensearchCommandLineProcess implements Closeable {
    private static final Logger LOG = LoggerFactory.getLogger(OpensearchCommandLineProcess.class);

    private final CommandLineProcess commandLineProcess;
    private final CommandLineProcessListener resultHandler;
    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    private static final Path CONFIG = Path.of("opensearch.yml");

    /**
     * as long as OpenSearch is not supported on macOS, we have to fix the jdk path if we want to
     * start the DataNode inside IntelliJ.
     *
     * @param config
     */
    private void fixJdkOnMac(final OpensearchConfiguration config) {
        final var isMacOS = OS.isFamilyMac();
        final var jdk = config.opensearchDistribution().directory().resolve("jdk.app");
        final var jdkNotLinked = !Files.exists(jdk);
        if (isMacOS && jdkNotLinked) {
            // Link System jdk into startup folder, get path:
            final ProcessBuilder builder = new ProcessBuilder("/usr/libexec/java_home");
            builder.redirectErrorStream(true);
            try {
                final Process process = builder.start();
                final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.defaultCharset()));
                var line = reader.readLine();
                if (line != null && Files.exists(Path.of(line))) {
                    final var target = Path.of(line);
                    final var src = Files.createDirectories(jdk.resolve("Contents"));
                    Files.createSymbolicLink(src.resolve("Home"), target);
                } else {
                    LOG.error("Output of '/usr/libexec/java_home' is not the jdk: {}", line);
                }
                // cleanup
                process.destroy();
                reader.close();
            } catch (IOException e) {
                LOG.error("Could not link jdk.app on macOS: {}", e.getMessage(), e);
            }
        }
    }

    private void writeOpenSearchConfig(final OpensearchConfiguration config) {
        try {
            final Path configFile = config.datanodeDirectories().createOpensearchProcessConfigurationFile(CONFIG);
            mapper.writeValue(configFile.toFile(), getOpensearchConfigurationArguments(config));
        } catch (IOException e) {
            throw new RuntimeException("Could not generate OpenSearch config: " + e.getMessage(), e);
        }
    }

    public OpensearchCommandLineProcess(OpensearchConfiguration config, ProcessListener listener) {
        fixJdkOnMac(config);
        configureS3RepositoryPlugin(config);
        final Path executable = config.opensearchDistribution().getOpensearchExecutable();
        writeOpenSearchConfig(config);
        resultHandler = new CommandLineProcessListener(listener);
        commandLineProcess = new CommandLineProcess(executable, List.of(), resultHandler, config.getEnv());
    }

    private void configureS3RepositoryPlugin(OpensearchConfiguration config) {
        if (config.s3RepositoryConfiguration().isRepositoryEnabled()) {
            final OpensearchCli opensearchCli = new OpensearchCli(config);
            configureS3Credentials(opensearchCli, config);
        } else {
            LOG.info("No S3 repository configuration provided, skipping plugin initialization");
        }
    }

    private void configureS3Credentials(OpensearchCli opensearchCli, OpensearchConfiguration config) {
        LOG.info("Creating opensearch keystore");
        final String createdMessage = opensearchCli.keystore().create();
        LOG.info(createdMessage);
        LOG.info("Setting opensearch s3 repository keystore secrets");
        opensearchCli.keystore().add("s3.client.default.access_key", config.s3RepositoryConfiguration().getS3ClientDefaultAccessKey());
        opensearchCli.keystore().add("s3.client.default.secret_key", config.s3RepositoryConfiguration().getS3ClientDefaultSecretKey());
    }

    private static Map<String, Object> getOpensearchConfigurationArguments(OpensearchConfiguration config) {
        Map<String, Object> allArguments = new LinkedHashMap<>(config.asMap());

        // now copy all the environment values to the configuration arguments. Opensearch won't do it for us,
        // because we are using tar distriburion and opensearch does this only for docker dist. See opensearch-env script
        // additionally, the env variables have to be prefixed with opensearch. (e.g. "opensearch.cluster.routing.allocation.disk.threshold_enabled")
        config.getEnv().getEnv().entrySet().stream()
                .filter(entry -> entry.getKey().matches("^opensearch\\.[a-z0-9_]+(?:\\.[a-z0-9_]+)+"))
                .peek(entry -> LOG.info("Detected pass-through opensearch property {}:{}", entry.getKey().substring("opensearch.".length()), entry.getValue()))
                .forEach(entry -> allArguments.put(entry.getKey().substring("opensearch.".length()), entry.getValue()));
        return allArguments;
    }

    public void start() {
        commandLineProcess.start();
    }

    @Override
    public void close() {
        commandLineProcess.stop();
        resultHandler.stopListening();
        waitForProcessTermination();
    }

    private void waitForProcessTermination() {
        try {
            RetryerBuilder.newBuilder()
                    .retryIfResult(Boolean.TRUE::equals)
                    .withWaitStrategy(WaitStrategies.fixedWait(100, TimeUnit.MILLISECONDS))
                    .withStopStrategy(StopStrategies.stopAfterDelay(60, TimeUnit.SECONDS))
                    .withRetryListener(new RetryListener() {
                        @Override
                        public <V> void onRetry(Attempt<V> attempt) {
                            LOG.info("Process " + commandLineProcess.processInfo().pid() + " still alive, waiting for termination.  Retry #" + attempt.getAttemptNumber());
                        }
                    })
                    .build()
                    .call(() -> commandLineProcess.processInfo().alive());
            LOG.info("Process " + commandLineProcess.processInfo().pid() + " successfully terminated.");
        } catch (ExecutionException | RetryException e) {
            final String message = "Failed to terminate opensearch process " + commandLineProcess.processInfo().pid();
            LOG.error(message, e);
            throw new RuntimeException(message, e);
        }
    }

    @NotNull
    public ProcessInformation processInfo() {
        return commandLineProcess.processInfo();
    }
}

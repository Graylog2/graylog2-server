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
package org.graylog.datanode.opensearch.cli;

import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.RetryListener;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.exec.OS;
import org.graylog.datanode.configuration.OpensearchConfigurationDir;
import org.graylog.datanode.configuration.OpensearchConfigurationException;
import org.graylog.datanode.opensearch.configuration.OpensearchConfiguration;
import org.graylog.datanode.process.CommandLineProcess;
import org.graylog.datanode.process.CommandLineProcessListener;
import org.graylog.datanode.process.ProcessInformation;
import org.graylog.datanode.process.ProcessListener;
import org.graylog.datanode.process.configuration.files.DatanodeConfigFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class OpensearchCommandLineProcess implements Closeable {
    private static final Logger LOG = LoggerFactory.getLogger(OpensearchCommandLineProcess.class);

    private final CommandLineProcess commandLineProcess;
    private final CommandLineProcessListener resultHandler;


    /**
     * as long as OpenSearch is not supported on macOS, we have to fix the jdk path if we want to
     * start the DataNode inside IntelliJ.
     *
     * @param config
     */
    private void fixJdkOnMac(final OpensearchConfiguration config) {
        final var isMacOS = OS.isFamilyMac();
        final var jdk = config.getOpensearchDistribution().directory().resolve("jdk.app");
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
        final OpensearchConfigurationDir confDir = config.getOpensearchConfigurationDir();
        config.configFiles().forEach(cf -> persistConfigFile(confDir, cf));
    }

    private static void persistConfigFile(OpensearchConfigurationDir confDir, DatanodeConfigFile cf) {
        try {
            final Path targetFile = confDir.createOpensearchProcessConfigurationFile(cf.relativePath());
            try (final FileOutputStream file = new FileOutputStream(targetFile.toFile())) {
                cf.write(file);
            }
        } catch (IOException e) {
            throw new OpensearchConfigurationException("Failed to create opensearch config file " + cf.relativePath(), e);
        }
    }

    public OpensearchCommandLineProcess(OpensearchConfiguration config, ProcessListener listener) {
        fixJdkOnMac(config);
        configureOpensearchKeystoreSecrets(config);
        final Path executable = config.getOpensearchDistribution().getOpensearchExecutable();
        writeOpenSearchConfig(config);
        resultHandler = new CommandLineProcessListener(listener);
        commandLineProcess = new CommandLineProcess(executable, List.of(), resultHandler, config.getEnv());
    }

    private void configureOpensearchKeystoreSecrets(OpensearchConfiguration config) {
        final OpensearchCli opensearchCli = new OpensearchCli(config);
        LOG.info("Creating opensearch keystore");
        final String createdMessage = opensearchCli.keystore().create();
        LOG.info(createdMessage);
        final Map<String, String> keystoreItems = config.getKeystoreItems();
        keystoreItems.forEach((key, value) -> opensearchCli.keystore().add(key, value));
        LOG.info("Added {} keystore items", keystoreItems.size());
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

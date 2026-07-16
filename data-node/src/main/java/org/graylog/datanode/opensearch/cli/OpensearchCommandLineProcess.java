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
import org.graylog.datanode.configuration.OpensearchConfigurationDir;
import org.graylog.datanode.configuration.OpensearchConfigurationException;
import org.graylog.datanode.configuration.variants.OpensearchCertificates;
import org.graylog.datanode.opensearch.configuration.OpensearchConfiguration;
import org.graylog.datanode.opensearch.configuration.beans.impl.OpensearchSecurityConfigurationBean;
import org.graylog.datanode.process.CommandLineProcess;
import org.graylog.datanode.process.CommandLineProcessListener;
import org.graylog.datanode.process.ProcessInformation;
import org.graylog.datanode.process.ProcessListener;
import org.graylog.datanode.process.configuration.beans.OpensearchKeystoreItem;
import org.graylog.datanode.process.configuration.files.DatanodeConfigFile;
import org.graylog.datanode.process.configuration.files.KeystoreConfigFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class OpensearchCommandLineProcess implements Closeable {
    private static final Logger LOG = LoggerFactory.getLogger(OpensearchCommandLineProcess.class);

    private final CommandLineProcess commandLineProcess;
    private final CommandLineProcessListener resultHandler;
    private final OpensearchConfiguration config;

    private void writeOpenSearchConfig(final OpensearchConfiguration config) {
        final OpensearchConfigurationDir confDir = config.getOpensearchConfigTargetDir();
        config.configFiles().forEach(cf -> persistConfigFile(confDir, cf));
        persistCertificates(config);
    }

    private void persistCertificates(OpensearchConfiguration config) {
        config.certificates().filter(OpensearchCertificates::hasCertificates).ifPresent(certificates -> persistCertificates(config.getOpensearchConfigTargetDir(), certificates));
    }

    private void persistCertificates(OpensearchConfigurationDir confDir, OpensearchCertificates certificates) {
        final KeystoreConfigFile httpFile = OpensearchCertificateFile.create(OpensearchSecurityConfigurationBean.TARGET_DATANODE_HTTP_KEYSTORE_FILENAME, certificates.getHttpKeystore(), certificates.getPassword());
        final KeystoreConfigFile transportFile = OpensearchCertificateFile.create(OpensearchSecurityConfigurationBean.TARGET_DATANODE_TRANSPORT_KEYSTORE_FILENAME, certificates.getTransportKeystore(), certificates.getPassword());
        persistConfigFile(confDir, httpFile);
        persistConfigFile(confDir, transportFile);
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
        this.config = config;
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
        final Collection<OpensearchKeystoreItem> keystoreItems = config.getKeystoreItems();
        keystoreItems.forEach((item) -> item.persist(opensearchCli.keystore()));
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
        final long pid = commandLineProcess.processInfo().pid();
        if (waitUntilTerminated(60)) {
            LOG.info("Process " + pid + " successfully terminated.");
            return;
        }

        LOG.warn("Process " + pid + " didn't terminate gracefully within timeout, forcing termination.");
        commandLineProcess.destroyForcibly();

        if (!waitUntilTerminated(10)) {
            final String message = "Failed to terminate opensearch process " + pid;
            LOG.error(message);
            throw new RuntimeException(message);
        }
        LOG.info("Process " + pid + " forcibly terminated.");
    }

    private boolean waitUntilTerminated(long timeoutSeconds) {
        try {
            RetryerBuilder.newBuilder()
                    .retryIfResult(Boolean.TRUE::equals)
                    .withWaitStrategy(WaitStrategies.fixedWait(100, TimeUnit.MILLISECONDS))
                    .withStopStrategy(StopStrategies.stopAfterDelay(timeoutSeconds, TimeUnit.SECONDS))
                    .withRetryListener(new RetryListener() {
                        @Override
                        public <V> void onRetry(Attempt<V> attempt) {
                            LOG.info("Process " + commandLineProcess.processInfo().pid() + " still alive, waiting for termination.  Retry #" + attempt.getAttemptNumber());
                        }
                    })
                    .build()
                    .call(() -> commandLineProcess.processInfo().alive());
            return true;
        } catch (ExecutionException | RetryException e) {
            return false;
        }
    }

    @NotNull
    public ProcessInformation processInfo() {
        return commandLineProcess.processInfo();
    }

    public void hotReload() {
        LOG.info("Triggered hot reload of opensearch certificates");
        persistCertificates(config);
    }
}

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
package org.graylog.datanode;

import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.RetryListener;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class DataNodeRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DataNodeRunner.class);

    private final Path opensearchLocation;
    private final Map<String, String> opensearchConfiguration;

    public DataNodeRunner(Path opensearchLocation, Map<String, String> opensearchConfiguration) {
        this.opensearchLocation = opensearchLocation;
        this.opensearchConfiguration = opensearchConfiguration;
    }

    public RunningProcess start() {
        try {
            setConfiguration(opensearchLocation, opensearchConfiguration);
            return run(opensearchLocation);
        } catch (IOException | InterruptedException | ExecutionException | RetryException e) {
            throw new RuntimeException(e);
        }
    }

    private void setConfiguration(Path targetLocation, Map<String, String> opensearchOptions) throws IOException {
        final Path configPath = targetLocation.resolve(Path.of("config", "opensearch.yml"));
        File file = configPath.toFile();
        try (
                FileWriter fr = new FileWriter(file, StandardCharsets.UTF_8);
                BufferedWriter br = new BufferedWriter(fr);
        ) {
            for (Map.Entry<String, String> option : opensearchOptions.entrySet()) {
                final String optionLine = toOptionLine(option);
                LOG.info("Setting configuration: " + option);
                br.write(optionLine + "\n");
            }
        }
    }

    private String toOptionLine(Map.Entry<String, String> option) {
        return option.getKey() + ": " + option.getValue();
    }

    private RunningProcess run(Path targetLocation) throws IOException, InterruptedException, ExecutionException, RetryException {
        final Path binPath = targetLocation.resolve(Paths.get("bin", "opensearch"));
        LOG.info("Running opensearch from " + binPath.toAbsolutePath());
        ProcessBuilder builder = new ProcessBuilder(binPath.toAbsolutePath().toString());
        builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        builder.redirectError(ProcessBuilder.Redirect.INHERIT);
        Process process = builder.start();

        awaitHttpApi();

        return new RunningProcess(process);

    }

    private void awaitHttpApi() throws ExecutionException, RetryException {

        final Retryer<Void> retryer = RetryerBuilder.<Void>newBuilder()
                .retryIfExceptionOfType(RuntimeException.class)
                .withWaitStrategy(WaitStrategies.fixedWait(1, TimeUnit.SECONDS))
                .withStopStrategy(StopStrategies.stopAfterDelay(10, TimeUnit.MINUTES))
                .withRetryListener(new RetryListener() {
                    @Override
                    public <V> void onRetry(Attempt<V> attempt) {
                        if (attempt.hasException()) {
                            LOG.warn("Waiting for opensearch instance, retry {}", attempt.getAttemptNumber());
                        }
                    }
                })
                .build();

        retryer.call(() -> {
            checkOpensearchStatus();
            return null;
        });

        LOG.info("Opensearch available on port 9200");
    }

    private void checkOpensearchStatus() throws RuntimeException {
        try {
            final URL url = new URL("http://localhost:9200");
            final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            try {
                if (connection.getResponseCode() != 200) {
                    throw new RuntimeException("Failed to obtain OS status");
                }
            } finally {
                connection.disconnect();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

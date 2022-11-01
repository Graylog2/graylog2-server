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
package org.graylog.testing.graylognode;

import com.github.dockerjava.api.command.LogContainerCmd;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.output.FrameConsumerResultCallback;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.WaitingConsumer;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

public class WaitForSuccessOrFailureStrategy extends AbstractWaitStrategy {

    private String success;
    private String failure;

    @Override
    protected void waitUntilReady() {
        WaitingConsumer waitingConsumer = new WaitingConsumer();

        LogContainerCmd cmd = waitStrategyTarget
                .getDockerClient()
                .logContainerCmd(waitStrategyTarget.getContainerId())
                .withFollowStream(true)
                .withSince(0)
                .withStdOut(true)
                .withStdErr(true);

        try (FrameConsumerResultCallback callback = new FrameConsumerResultCallback()) {
            callback.addConsumer(OutputFrame.OutputType.STDOUT, waitingConsumer);
            callback.addConsumer(OutputFrame.OutputType.STDERR, waitingConsumer);

            cmd.exec(callback);

            Predicate<OutputFrame> waitPredicate = outputFrame -> {
                // (?s) enables line terminator matching (equivalent to Pattern.DOTALL)
                if(outputFrame.getUtf8String().matches("(?s)" + failure)) {
                    throw new ContainerLaunchException("Container startup failed. Was looking for: '" + failure + "'");
                }
                return outputFrame.getUtf8String().matches("(?s)" + success);
            };
            try {
                waitingConsumer.waitUntil(waitPredicate, startupTimeout.getSeconds(), TimeUnit.SECONDS, 1);
            } catch (TimeoutException e) {
                throw new ContainerLaunchException("Timed out waiting for log output matching '" + success + "' or '" + failure + "'");
            }
        } catch (IOException iox) {
            throw new ContainerLaunchException("Failed with Exception: " + iox.getMessage());
        }
    }

    public WaitForSuccessOrFailureStrategy withSuccessAndFailure(String success, String failure) {
        this.success = success;
        this.failure = failure;
        return this;
    }
}

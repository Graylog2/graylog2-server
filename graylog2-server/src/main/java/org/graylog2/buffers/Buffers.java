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
package org.graylog2.buffers;

import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.base.Predicates;
import org.graylog2.plugin.buffers.EventBuffer;
import org.graylog2.plugin.buffers.InputBuffer;
import org.graylog2.shared.buffers.ProcessBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.graylog2.buffers.Buffers.Type.INPUT;
import static org.graylog2.buffers.Buffers.Type.OUTPUT;
import static org.graylog2.buffers.Buffers.Type.PROCESS;

public class Buffers {
    public enum Type {
        INPUT,
        PROCESS,
        OUTPUT,
    }

    private static final Logger LOG = LoggerFactory.getLogger(Buffers.class);
    private static final long DEFAULT_MAX_WAIT = 30L;

    private final InputBuffer inputBuffer;
    private final ProcessBuffer processBuffer;
    private final OutputBuffer outputBuffer;

    @Inject
    public Buffers(InputBuffer inputBuffer, final ProcessBuffer processBuffer, final OutputBuffer outputBuffer) {
        this.inputBuffer = inputBuffer;
        this.processBuffer = processBuffer;
        this.outputBuffer = outputBuffer;
    }

    /**
     * @deprecated Use {@link #waitForEmptyBuffers(EnumSet)} instead
     */
    @Deprecated
    public void waitForEmptyBuffers() {
        waitForEmptyBuffers(DEFAULT_MAX_WAIT, TimeUnit.SECONDS);
    }

    public void waitForEmptyBuffers(EnumSet<Type> bufferTypes) {
        waitForEmptyBuffers(EnumSet.of(PROCESS, OUTPUT), DEFAULT_MAX_WAIT, TimeUnit.SECONDS);
    }

    /**
     * @deprecated Usse {@link #waitForEmptyBuffers(EnumSet, long, TimeUnit)} instead
     */
    @Deprecated
    public void waitForEmptyBuffers(final long maxWait, final TimeUnit timeUnit) {
        waitForEmptyBuffers(EnumSet.of(PROCESS, OUTPUT), maxWait, timeUnit);
    }

    /**
     * Wait until the buffers of the given types have been drained or abort after a given maximum waiting time
     */
    public void waitForEmptyBuffers(EnumSet<Type> bufferTypes, final long maxWait, final TimeUnit timeUnit) {
        if (bufferTypes.isEmpty()) {
            return;
        }
        LOG.info("Waiting until {} buffers are empty.", bufferTypes);

        final Map<Type, EventBuffer> buffersByTypes = buffersByTypes(bufferTypes);

        final Retryer<Boolean> retryer = RetryerBuilder.<Boolean>newBuilder()
                .retryIfResult(Predicates.not(Predicates.equalTo(Boolean.TRUE)))
                .withWaitStrategy(WaitStrategies.fixedWait(1, TimeUnit.SECONDS))
                .withStopStrategy(StopStrategies.stopAfterDelay(maxWait, timeUnit))
                .build();

        try {
            retryer.call(() -> {
                if (buffersByTypes.values().stream().allMatch(EventBuffer::isEmpty)) {
                    return true;
                } else {
                    LOG.info("Waiting for buffers to drain. ({})", getUsageStats(buffersByTypes));
                }
                return false;
            });
        } catch (RetryException e) {
            LOG.info("Buffers not empty after {} {}. Giving up.", maxWait, timeUnit.name().toLowerCase(Locale.ENGLISH));
            return;
        } catch (ExecutionException e) {
            LOG.error("Error while waiting for empty buffers.", e);
            return;
        }

        LOG.info("All buffers are empty. Continuing.");
    }

    // e.g. 123i/432p/545o
    private String getUsageStats(Map<Type, EventBuffer> buffersByTypes) {
        return buffersByTypes.entrySet().stream()
                .map(e -> e.getValue().getUsage() + e.getKey().name().substring(0, 1).toLowerCase(Locale.ENGLISH))
                .collect(Collectors.joining("/"));
    }

    private Map<Type, EventBuffer> buffersByTypes(EnumSet<Type> bufferTypes) {
        Map<Type, EventBuffer> bufferMap = new LinkedHashMap<>();
        if (bufferTypes.contains(INPUT)) {
            bufferMap.put(INPUT, inputBuffer);
        }
        if (bufferTypes.contains(PROCESS)) {
            bufferMap.put(PROCESS, processBuffer);
        }
        if (bufferTypes.contains(OUTPUT)) {
            bufferMap.put(OUTPUT, outputBuffer);
        }
        return bufferMap;
    }
}

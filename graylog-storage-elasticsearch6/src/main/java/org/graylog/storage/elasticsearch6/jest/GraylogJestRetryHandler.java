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
package org.graylog.storage.elasticsearch6.jest;

import com.google.common.base.Preconditions;
import io.searchbox.client.JestRetryHandler;
import org.apache.http.ConnectionClosedException;
import org.apache.http.client.methods.HttpUriRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;

public class GraylogJestRetryHandler implements JestRetryHandler<HttpUriRequest> {
    private static final Logger log = LoggerFactory.getLogger(GraylogJestRetryHandler.class);

    private final int retryCount;
    private final Collection<Class<? extends Exception>> exceptionClasses = Arrays.asList(
        UnknownHostException.class,
        SocketException.class,
        SocketTimeoutException.class,
        ConnectionClosedException.class,
        SSLException.class);

    public GraylogJestRetryHandler(int retryCount) {
        Preconditions.checkArgument(retryCount >= 0, "retryCount must be positive");
        this.retryCount = retryCount;
    }

    @Override
    public boolean retryRequest(Exception exception, int executionCount, HttpUriRequest request) {
        if (executionCount >= retryCount) {
            log.debug("Maximum number of retries ({}) for request {} reached (executed {} times) (Reason: {})",
                retryCount, request, executionCount, exception.getMessage());
            return false;
        } else {
            for (Class<? extends Exception> exceptionClass : exceptionClasses) {
                if (exceptionClass.isInstance(exception)) {
                    log.debug("Retrying request {} (Reason: {})", request, exception.getMessage());
                    return true;
                }
            }

            log.debug("Not retrying request {} due to unsupported exception (Reason: {})", request, exception.getMessage());
            return false;
        }
    }
}

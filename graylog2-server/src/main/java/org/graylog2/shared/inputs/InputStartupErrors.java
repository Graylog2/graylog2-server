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
package org.graylog2.shared.inputs;

import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.shared.utilities.ExceptionUtils;

import javax.annotation.Nullable;
import javax.net.ssl.SSLException;
import java.net.BindException;
import java.net.UnknownHostException;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Produces user-facing error messages for input startup failures.
 * Dispatches on the root cause exception type and enriches the message
 * with bind address and port from the input configuration when available.
 */
public class InputStartupErrors {

    private InputStartupErrors() {
    }

    /**
     * Build an actionable error description for a failed input startup.
     *
     * @param input the input that failed to start
     * @param e     the exception caught during startup
     * @return a user-facing error message
     */
    public static String describeFailure(MessageInput input, Throwable e) {
        final Throwable root = ExceptionUtils.getRootCause(e);
        final String bindAddress = getBindAddress(input);
        final int port = getPort(input);
        final boolean hasAddress = bindAddress != null && port > 0;

        if (root instanceof BindException && hasAddress) {
            final String rootMsg = root.getMessage();
            if (rootMsg != null && rootMsg.contains("Address already in use")) {
                return f("Port %d is already in use. Check if another input or process is bound to %s:%d.",
                        port, bindAddress, port);
            }
            return f("Cannot bind to %s:%d. Verify the address is available on this host.",
                    bindAddress, port);
        }

        if (root instanceof UnknownHostException) {
            final String host = root.getMessage();
            return host != null ? f("Unknown host '%s'.", host) : "Unknown host.";
        }

        if (root instanceof SSLException && hasAddress) {
            return f("TLS configuration error on %s:%d. %s",
                    bindAddress, port, ExceptionUtils.getRootCauseOrMessage(e));
        }

        if (hasAddress) {
            return f("Startup failed on %s:%d. %s",
                    bindAddress, port, ExceptionUtils.getRootCauseOrMessage(e));
        }

        return ExceptionUtils.getRootCauseOrMessage(e);
    }

    @Nullable
    private static String getBindAddress(MessageInput input) {
        try {
            return input.getConfiguration().getString("bind_address");
        } catch (Exception e) {
            return null;
        }
    }

    private static int getPort(MessageInput input) {
        try {
            final var config = input.getConfiguration();
            return config.intIsSet("port") ? config.getInt("port") : -1;
        } catch (Exception e) {
            return -1;
        }
    }
}

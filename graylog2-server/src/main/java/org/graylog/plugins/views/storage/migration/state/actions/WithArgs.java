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
package org.graylog.plugins.views.storage.migration.state.actions;

import java.util.Map;

/**
 * This allows handling request parameters inside {@link MigrationActions}. The original state machine
 * doesn't support action parameters and typed triggers are clumsy to use. In the same time there is no
 * simple way to extend the state machine to support parameters in transition actions.
 *
 * With this interface, we support them inside {@link MigrationActions} where we actually need them.
 */
public interface WithArgs {
    ThreadLocal<Map<String, Object>> requestArguments = new ThreadLocal<>();

    /**
     * @return request arguments provided by the frontend to the state machine transition action
     */
    default Map<String, Object> args() {
        return requestArguments.get();
    }

    default void setArgs(Map<String, Object> args) {
        requestArguments.set(args);
    }

    default void clearArgs() {
        requestArguments.remove();
    }
}

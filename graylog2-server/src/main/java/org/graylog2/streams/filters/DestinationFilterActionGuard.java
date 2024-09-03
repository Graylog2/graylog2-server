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
package org.graylog2.streams.filters;

/**
 * An interface that acts as an destination filter action guard, enforcing permissions and throwing an {@link DestinationFilterActionException}
 * when unauthorized actions are attempted.
 *
 */
public interface DestinationFilterActionGuard {

    /**
     * This method is intended to be used before executing actions that require extended validation
     * or authorization. If the action is not allowed, an DestinationFilterActionException should be thrown to
     * prevent further execution.
     *
     * @param actionType action to verify
     * @throws DestinationFilterActionException if the action is not permitted or fails validation
     */
    void checkAction(ActionType actionType) throws DestinationFilterActionException;

    enum ActionType {
        CREATE
    }
}

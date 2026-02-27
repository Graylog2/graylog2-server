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
 * An interface for validating whether the creation of a new destination filter should be allowed; otherwise, it throws an {@link IllegalStateException}
 *
 */
public interface DestinationFilterCreationValidator {

    /**
     * This method checks if the specified criteria for creating a new destination filter are met.
     * If the creation is not allowed, an appropriate exception is thrown.
     *
     * @param destinationFilterRuleDTO dto that should be verified
     * @throws IllegalStateException if the creating filter is not permitted
     */
    void validate(StreamDestinationFilterRuleDTO destinationFilterRuleDTO) throws IllegalStateException;
}

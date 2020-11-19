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
package org.graylog2.inputs.persistence;

import java.util.Optional;

/**
 * Provides CRUD operations for input status records.
 */
public interface InputStatusService {
    /**
     * Get the saved status record for an Input.
     *
     * @param inputId ID of the input whose status you want to get
     * @return The InputStatusRecord for the given Input ID if it exists; otherwise an empty Optional.
     */
    Optional<InputStatusRecord> get(String inputId);

    /**
     * Save the status record for an Input.
     *
     * @param statusRecord The Input status record to save
     * @return A copy of the saved object
     */
    InputStatusRecord save(InputStatusRecord statusRecord);

    /**
     * Remove the status record for a given Input
     *
     * @param inputId ID of the input whose status you want to delete
     * @return The count of deleted objects (should be 0 or 1)
     */
    int delete(String inputId);
}

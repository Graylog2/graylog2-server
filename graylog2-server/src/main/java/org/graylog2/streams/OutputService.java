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
package org.graylog2.streams;

import com.google.inject.ImplementedBy;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.streams.Output;
import org.graylog2.rest.models.streams.outputs.requests.CreateOutputRequest;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

@ImplementedBy(OutputServiceImpl.class)
public interface OutputService {
    Output load(String streamOutputId) throws NotFoundException;

    Set<Output> loadByIds(Collection<String> ids);

    Set<Output> loadAll();

    Output create(Output request) throws ValidationException;

    Output create(CreateOutputRequest request, String userId) throws ValidationException;

    void destroy(Output model) throws NotFoundException;

    Output update(String id, Map<String, Object> deltas);

    /**
     * @return the total number of outputs
     */
    long count();

    /**
     * @return the total number of outputs grouped by type
     */
    Map<String, Long> countByType();
}

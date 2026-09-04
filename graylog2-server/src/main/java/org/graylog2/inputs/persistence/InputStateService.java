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

import org.graylog2.plugin.IOState;
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface InputStateService {

    void upsertState(String inputId, IOState.Type state,
                     @Nullable DateTime startedAt,
                     @Nullable DateTime lastFailedAt,
                     @Nullable String detailedMessage);

    void removeState(String inputId);

    void removeAllForNode();

    void removeAllForNode(String nodeId);

    Map<String, Set<String>> getClusterStatuses();

    Set<String> getByState(IOState.Type state);

    Set<InputStateDto> getByStates(Collection<IOState.Type> states);

    Set<String> getDistinctNodeIds();
}

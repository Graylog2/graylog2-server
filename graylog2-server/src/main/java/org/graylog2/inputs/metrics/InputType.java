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
package org.graylog2.inputs.metrics;

import java.util.Set;

/**
 * Pluggable description of a kind of input that can appear in messages and metrics. Each registered
 * implementation owns one MongoDB collection (e.g. open-source {@code inputs}, enterprise
 * {@code forwarder_inputs}) and declares both the read permission for that collection and how to
 * filter a candidate ID set down to the IDs that actually exist there.
 *
 * <p>Used by metric descriptors to attach a stable {@link TypedInputId#type()} to each input ID at
 * compute time, so that per-user permission filtering can run without additional DB lookups.</p>
 */
public interface InputType {
    /**
     * Stable string identifier for this input type. Appears as {@link TypedInputId#type()} and as
     * the discriminator in REST responses (e.g. {@code "input"}, {@code "forwarder_input"}).
     */
    String typeName();

    /**
     * Shiro read permission name guarding inputs of this type
     * (e.g. {@code "inputs:read"}, {@code "forwarderinputs:read"}).
     */
    String readPermission();

    /**
     * Returns the subset of {@code candidateIds} that actually exist in this type's backing
     * collection. Callers must pre-filter to valid {@code ObjectId} hex — implementations issue
     * a direct {@code findByIds} and rely on that guarantee.
     */
    Set<String> filterMembers(Set<String> candidateIds);
}

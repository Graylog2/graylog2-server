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
package org.graylog.collectors.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Sets;

import java.util.Objects;
import java.util.Set;

/**
 * Cluster event signalling that the set of certificate fingerprints belonging to collector instances
 * has changed. {@code addedFingerprints} became valid; {@code removedFingerprints} are no longer
 * associated with an active instance.
 */
public record CollectorInstanceCertsChangedEvent(
        @JsonProperty("added_fingerprints") Set<String> addedFingerprints,
        @JsonProperty("removed_fingerprints") Set<String> removedFingerprints) {
    public CollectorInstanceCertsChangedEvent {
        Objects.requireNonNull(addedFingerprints, "addedFingerprints must not be null");
        Objects.requireNonNull(removedFingerprints, "removedFingerprints must not be null");
    }

    /**
     * Builds an event from an instance's fingerprints before and after a change: fingerprints only in
     * {@code after} are added, fingerprints only in {@code before} are removed.
     */
    public static CollectorInstanceCertsChangedEvent forDifference(Set<String> before, Set<String> after) {
        final Set<String> added = Sets.difference(after, before);
        final Set<String> removed = Sets.difference(before, after);
        return new CollectorInstanceCertsChangedEvent(added, removed);
    }
}

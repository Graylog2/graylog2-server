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

import java.util.Objects;
import java.util.Set;

/**
 * Cluster event signalling that a collector instance operation touched a set of certificate fingerprints
 * (enrollment, renewal insertion/activation, re-enrollment, or deletion). Subscribers re-resolve the
 * affected fingerprints against current state. The event carries the fingerprints only, not their new
 * binding — the resolver is the source of truth for what each fingerprint now maps to (or that it no
 * longer maps to anything).
 */
public record CollectorInstanceCertsChangedEvent(
        @JsonProperty("fingerprints") Set<String> fingerprints) {
    public CollectorInstanceCertsChangedEvent {
        Objects.requireNonNull(fingerprints, "fingerprints must not be null");
    }
}

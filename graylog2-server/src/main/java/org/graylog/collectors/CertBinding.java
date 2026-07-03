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
package org.graylog.collectors;

import java.time.Instant;
import java.util.Objects;

/**
 * A certificate fingerprint's binding to a collector instance: the instance the certificate binds to and
 * the instant the binding stops being honored on the ingest path — the certificate's own expiry, capped
 * for a superseded (previous-slot) certificate by the renewal grace deadline. The binding is a pure fact
 * about the instance's certificate slots; whether it is valid <em>now</em> is evaluated by the reader via
 * {@link #isValidAt(Instant)}, so a resolved binding may already be expired.
 */
public record CertBinding(String instanceUid, Instant validUntil) {
    public CertBinding {
        Objects.requireNonNull(instanceUid, "instanceUid must not be null");
        Objects.requireNonNull(validUntil, "validUntil must not be null");
    }

    public boolean isValidAt(Instant instant) {
        return instant.isBefore(validUntil());
    }
}

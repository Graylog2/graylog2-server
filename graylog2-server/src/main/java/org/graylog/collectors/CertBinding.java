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
import java.util.Optional;

/**
 * A certificate fingerprint's binding to a collector instance: the instance it binds to and — for a
 * superseded (previous-slot) certificate — the instant until which the binding remains valid
 * ({@code validUntil} empty means valid indefinitely, i.e. an active or next certificate). A
 * {@code CertBinding} is always bound; the absence of any binding is represented by an empty
 * {@link Optional} at the API boundary (see {@link CollectorInstanceService#resolveCertBinding(String)}).
 */
public record CertBinding(String instanceUid, Optional<Instant> validUntil) {
    public static CertBinding bound(String instanceUid) {
        return new CertBinding(instanceUid, Optional.empty());
    }

    public static CertBinding boundWithDeadline(String instanceUid, Instant validUntil) {
        return new CertBinding(instanceUid, Optional.of(validUntil));
    }
}

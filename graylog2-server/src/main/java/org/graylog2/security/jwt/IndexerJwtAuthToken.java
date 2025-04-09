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
package org.graylog2.security.jwt;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * This is a dynamic wrapper/proxy around actual jwt token supplier, because we want to prevent any accidental caching
 * of the actual token string in caller classes.
 * You can cache/reuse this class, but never reuse the header/token value itself.
 */
public class IndexerJwtAuthToken {

    private final Supplier<Optional<String>> tokenSupplier;

    public IndexerJwtAuthToken(Supplier<Optional<String>> tokenSupplier) {
        this.tokenSupplier = tokenSupplier;
    }

    /**
     * Create token proxy that doesn't contain any token and will always return empty optionals for both
     * header value and raw header token.
     */
    public static IndexerJwtAuthToken disabled() {
        return new IndexerJwtAuthToken(Optional::empty);
    }

    /**
     * @return readily usable auth header value. Already contains the "Bearer " prefix.
     */
    public Optional<String> headerValue() {
        return tokenSupplier.get().map(value -> "Bearer " + value);
    }

    /**
     * @return jwt token alone, encoded and parseable. Use if you don't want header value and need the jwt token elewhere.
     */
    public Optional<String> rawTokenValue() {
        return tokenSupplier.get();
    }

    public boolean isJwtAuthEnabled() {
        return tokenSupplier.get().isPresent();
    }
}

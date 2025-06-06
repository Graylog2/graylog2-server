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
package org.graylog2.bootstrap.preflight.web.resources;

import jakarta.annotation.Nullable;
import org.graylog2.storage.SearchVersion;

public record ConnectionCheckResult(@Nullable SearchVersion searchVersion, String errorMessage) {
    public static ConnectionCheckResult success(SearchVersion searchVersion) {
        return new ConnectionCheckResult(searchVersion, null);
    }

    public static ConnectionCheckResult failure(String error) {
        return new ConnectionCheckResult(null, error);
    }


    public boolean succeeded() {
        return searchVersion != null;
    }
}

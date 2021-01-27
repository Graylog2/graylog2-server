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
package org.graylog2.indexer;

import java.util.Collections;

public class InvalidWriteTargetException extends ElasticsearchException {

    private InvalidWriteTargetException(String target, Throwable cause) {
        super("Write target for indexing is invalid. This can happen if the deflector points to zero or multiple targets.", Collections.singletonList("target=" + target), cause);
    }

    public static InvalidWriteTargetException create(String target, Throwable cause) {
        return new InvalidWriteTargetException(target, cause);
    }

    public static InvalidWriteTargetException create(String target) {
        return new InvalidWriteTargetException(target, null);
    }
}

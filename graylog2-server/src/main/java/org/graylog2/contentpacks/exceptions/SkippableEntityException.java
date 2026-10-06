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
package org.graylog2.contentpacks.exceptions;

/**
 * Thrown by a facade during content pack installation when an entity cannot be resolved but the installation should
 * skip it and carry on, instead of aborting and rolling back the whole content pack.
 * <p>
 * This is the tolerant counterpart to {@link MissingNativeEntityException}: the latter signals a hard dependency that
 * must abort the install, whereas this one marks an optional reference (e.g. a {@code stream_title} pointing at a
 * stream that does not exist on the target system) that can safely be dropped. The install loop in
 * {@code ContentPackService} catches this exception per entity and continues; any other exception still aborts.
 */
public class SkippableEntityException extends ContentPackException {
    public SkippableEntityException(String message) {
        super(message);
    }
}

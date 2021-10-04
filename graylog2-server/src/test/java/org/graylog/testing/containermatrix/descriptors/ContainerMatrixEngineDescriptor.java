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
package org.graylog.testing.containermatrix.descriptors;

import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContainerMatrixEngineDescriptor extends EngineDescriptor {
    private static final Logger LOG = LoggerFactory.getLogger(ContainerMatrixEngineDescriptor.class);

    /**
     * Create a new {@code EngineDescriptor} with the supplied {@link UniqueId}
     * and display name.
     *
     * @param uniqueId    the {@code UniqueId} for the described {@code TestEngine};
     *                    never {@code null}
     * @param displayName the display name for the described {@code TestEngine};
     *                    never {@code null} or blank
     * @see TestEngine#getId()
     * @see TestDescriptor#getDisplayName()
     */
    public ContainerMatrixEngineDescriptor(UniqueId uniqueId, String displayName) {
        super(uniqueId, displayName);
    }
}

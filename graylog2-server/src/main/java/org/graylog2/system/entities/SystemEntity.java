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
package org.graylog2.system.entities;

import java.util.List;

/**
 * Specification for a Graylog entity.
 *
 * <p>
 * The objective for such entity is to identify entities that must be managed in a common way--such as controlling whether an entity can be edited or deleted.
 * </p>
 */
public interface SystemEntity {

    /**
     * The entity's unique ID.
     *
     * @return entity's ID
     */
    String id();

    /**
     * A list of actions which have been forbidden/denied on this entity. The lack of denied actions suggests that all actions are allowed for an entity.
     *
     * @return forbidden/denied actions
     */
    List<SystemEntityAction> deniedActions();
}

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
package org.graylog.scheduler.capabilities;

import java.util.Set;

/**
 * Interface for defining capabilities related to job definition types.
 * Implementations should specify which job definition types cannot be run by this node.
 * The reason for not running a job could be permanent or temporary.
 */
public interface JobDefinitionTypeCapability {

    Set<String> notCapableToRunJobDefinitionTypes();
}

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
package org.graylog.events.processor;

/**
 * Changes an event definition before it is validated and stored, e.g. to raise a value to a configured minimum.
 * <p>
 * Runs on create, update and validate in the event definitions REST API and when a content pack installs or updates
 * a definition. Other write paths, e.g. duplicating a definition or imports, skip it.
 * <p>
 * Register with {@code PluginModule#addEventDefinitionNormalizer}. Return the definition unchanged if it is not one
 * the normalizer handles.
 */
public interface EventDefinitionNormalizer {
    EventDefinitionDto normalize(EventDefinitionDto eventDefinition);
}

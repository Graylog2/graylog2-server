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
package org.graylog2.inputs;

import org.graylog2.plugin.database.Persisted;
import org.joda.time.DateTime;

import java.util.Map;

public interface Input extends Persisted {
    String getTitle();

    DateTime getCreatedAt();

    Map<String, Object> getConfiguration();

    Map<String, String> getStaticFields();

    String getType();

    String getCreatorUserId();

    Boolean isGlobal();

    String getContentPack();

    String getNodeId();
}

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
package org.graylog2.plugin.database;

import org.graylog2.plugin.database.validators.Validator;

import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public interface Persisted {
    String getId();

    Map<String, Object> getFields();
    Map<String, Validator> getValidations();
    Map<String, Validator> getEmbeddedValidations(String key);

    Map<String, Object> asMap();
}

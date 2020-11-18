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
package org.graylog2;

/**
 * Exception thrown in case of an invalid configuration
 *
 * @see Configuration
 *
 * @author Jochen Schalanda <jochen@schalanda.name>
 */
public class ConfigurationException extends Exception {

    private static final long serialVersionUID = -4307445842675210038L;

    public ConfigurationException(String message) {

        super(message);
    }
}

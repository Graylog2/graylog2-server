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
package org.graylog2.security;

import org.graylog2.Configuration;

/**
 * Exception indicating an attempt to load a class that is not considered safe because it's fully qualified class name
 * did not start with any of the prefixes configured in {@link Configuration#getSafeClasses()}
 */
public class UnsafeClassLoadingAttemptException extends Exception {
    public UnsafeClassLoadingAttemptException(String message) {
        super(message);
    }
}

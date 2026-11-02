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
package org.graylog2.plugin.rest;

import java.util.Map;

/**
 * An exception which carries a stable error code and the values of its placeholders next to the English message,
 * so that the web interface can show the error in the language of the user. The message stays the English text for
 * logs and API clients.
 */
public interface CodedException {
    /**
     * Stable identifier of the error, e.g. {@code stream.title_exists}.
     */
    String code();

    /**
     * Values of the placeholders of the translated error text.
     */
    Map<String, Object> params();
}

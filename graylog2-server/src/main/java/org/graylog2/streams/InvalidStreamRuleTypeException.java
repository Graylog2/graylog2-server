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
package org.graylog2.streams;

/**
 * Exception thrown in case of an invalid stream rule type. Allowed types are
 * defined in StreamRule.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class InvalidStreamRuleTypeException extends Exception {

    private static final long serialVersionUID = -4620424296979992728L;

}
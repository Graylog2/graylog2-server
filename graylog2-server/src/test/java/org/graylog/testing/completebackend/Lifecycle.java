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
package org.graylog.testing.completebackend;

/**
 * Controls the lifecycle of the {@link GraylogBackend} used in tests
 */
public enum Lifecycle {
    /**
     * {@link GraylogBackend} will be reused for all tests in the running VM. Use this, if you can make sure
     * that the individual tests will not interfere with each other, e.g., by creating test data that
     * would affect the outcome of a different test.
     */
    VM,
    /**
     * {@link GraylogBackend} will be reused for all tests in a class. Use this, if you can make sure
     * that the individual tests will not interfere with each other, e.g., by creating test data that
     * would affect the outcome of a different test.
     */
    CLASS
}

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
package org.graylog2.plugin.database.validators;

import com.google.common.base.MoreObjects;

public abstract class ValidationResult {
    public abstract boolean passed();

    public static class ValidationPassed extends ValidationResult {
        @Override
        public boolean passed() {
            return true;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                .add("passed", passed())
                .toString();
        }
    }

    public static class ValidationFailed extends ValidationResult {
        private final String error;

        public ValidationFailed(String errors) {
            this.error = errors;
        }

        public String getError() {
            return error;
        }

        @Override
        public boolean passed() {
            return false;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                .add("passed", passed())
                .add("error", getError())
                .toString();
        }
    }
}

/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
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

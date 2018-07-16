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
package org.graylog2.contentpacks.exceptions;

import org.graylog2.contentpacks.model.entities.references.ValueType;

import static java.util.Objects.requireNonNull;

public class InvalidParameterTypeException extends ContentPackException {
    private final ValueType expectedValueType;
    private final ValueType actualValueType;

    public InvalidParameterTypeException(ValueType expectedValueType, ValueType actualValueType) {
        super("Incompatible value types, content pack expected " + expectedValueType + ", parameters provided " + actualValueType);
        this.expectedValueType = requireNonNull(expectedValueType, "expectedValueType");
        this.actualValueType = requireNonNull(actualValueType, "actualValueType");
    }

    public ValueType getExpectedValueType() {
        return expectedValueType;
    }

    public ValueType getActualValueType() {
        return actualValueType;
    }
}

/*
 * Copyright 2012-2014 TORCH GmbH
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.graylog2.database.validators;

public class LimitedStringValidator extends FilledStringValidator {
    private int minLength;
    private int maxLength;

    public LimitedStringValidator(int minLength, int maxLength) {
        this.minLength = minLength;
        this.maxLength = maxLength;
    }

    /**
     * Validates: applies the validation from FilledStringValidator and also check that value's length
     * is between the minimum and maximum length passed to the constructor.
     *
     * @param value The object to check
     * @return validation result
     */
    @Override
    public boolean validate(Object value) {
        if (super.validate(value)) {
            String sValue = (String)value;
            return sValue.length() >= minLength && sValue.length() <= maxLength;
        }
        return false;
    }
}

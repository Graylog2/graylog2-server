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
package org.graylog2.configuration.validators;

import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.Validator;

import java.util.Arrays;
import java.util.List;

public class HttpOrHttpsSchemeValidator implements Validator<String> {

    private static final List<String> validScheme = Arrays.asList("http", "https");

    @Override
    public void validate(String name, String value) throws ValidationException {
        if (!validScheme.contains(value.toLowerCase())) {
            throw new ValidationException(String.format("Parameter " + name + " must be one of [%s]", String.join(",")));
        }
    }
}

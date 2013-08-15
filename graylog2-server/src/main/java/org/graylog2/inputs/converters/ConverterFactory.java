/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
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
 *
 */
package org.graylog2.inputs.converters;

import org.graylog2.plugin.inputs.Converter;

import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class ConverterFactory {

    public static Converter factory(Converter.Type type, Map<String, Object> config) throws NoSuchConverterException {
        switch (type) {
            case NUMERIC:
                return new NumericConverter(config);
            case DATE:
                throw new NoSuchConverterException();
            case HASH:
                return new HashConverter(config);
            default:
                throw new NoSuchConverterException();
        }
    }

    public static class NoSuchConverterException extends Throwable {
    }
}

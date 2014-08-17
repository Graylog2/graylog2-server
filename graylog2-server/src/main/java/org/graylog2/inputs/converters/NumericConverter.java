/**
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
package org.graylog2.inputs.converters;

import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import org.graylog2.plugin.inputs.Converter;

import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class NumericConverter extends Converter {

    public NumericConverter(Map<String, Object> config) {
        super(Type.NUMERIC, config);
    }

	/**
	 * Attempts to convert the provided string value to a numeric type,
	 * trying Integer, Long and Double in order until successful.
	 */
    @Override
    public Object convert(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        Object result = Ints.tryParse(value);

        if (result != null) {
            return result;
        }

        result = Longs.tryParse(value);

        if (result != null) {
            return result;
        }

        result = Doubles.tryParse(value);

        if (result != null) {
            return result;
        }

        return value;
    }

    @Override
    public boolean buildsMultipleFields() {
        return false;
    }

}

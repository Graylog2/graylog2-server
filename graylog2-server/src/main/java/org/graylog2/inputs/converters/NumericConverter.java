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

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
package org.graylog.datanode;

import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.Validator;

public class JavaHeapSizeValidator implements Validator<String> {

    @Override
    public void validate(String name, String value) throws ValidationException {
        if (value == null || !value.matches("\\d+[gGmMkK]")) {
            throw new ValidationException("Invalid heap size configuration: " + value + ". Set " + name + " to <size>[g|G|m|M|k|K]. For example 4g or 512m.");
        }
    }
}

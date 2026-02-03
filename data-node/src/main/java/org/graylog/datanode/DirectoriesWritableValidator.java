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

import java.nio.file.Path;
import java.util.List;

/**
 * The original validator doesn't tell if the problem is nonexistence of the directory, file instead of dir
 * or permissions problem. This validator provides more specific exception with detailed message.
 */
public class DirectoriesWritableValidator implements Validator<List<Path>> {

    public static final DirectoryWritableValidator DIRECTORY_WRITABLE_VALIDATOR = new DirectoryWritableValidator();

    @Override
    public void validate(String name, List<Path> value) throws ValidationException {
        for (Path dir : value) {
            DIRECTORY_WRITABLE_VALIDATOR.validate(name, dir);
        }
    }
}

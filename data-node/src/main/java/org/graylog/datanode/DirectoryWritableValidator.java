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
import com.github.joschi.jadconfig.validators.DirectoryPathWritableValidator;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The original validator doesn't tell if the problem is nonexistence of the directory, file instead of dir
 * or permissions problem. This validator provides more specific exception with detailed message.
 */
public class DirectoryWritableValidator extends DirectoryPathWritableValidator {
    @Override
    public void validate(String name, Path value) throws ValidationException {
        if (value == null) {
            return;
        }

        if (!Files.exists(value)) {
            throw new ValidationException("Cannot write to directory " + name + " at path " + value + ". Directory doesn't exist. Please create the directory.");
        }

        if (!Files.isDirectory(value)) {
            throw new ValidationException("Cannot write to directory " + name + " at path " + value + ". Referenced path is not a directory.");
        }

        if (!Files.isWritable(value)) {
            if (Files.isReadable(value)) {
                throw new ValidationException("Cannot write to directory " + name + " at path " + value + ". Directory only readable. Please set also write permissions.");
            } else {
                throw new ValidationException("Cannot write to directory " + name + " at path " + value + ". Directory neither readable not writable. Please set read and write permission.");
            }
        }
    }
}

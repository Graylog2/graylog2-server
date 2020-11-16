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
package org.graylog2;

import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.factory.DefaultDirectoryServiceFactory;
import org.apache.directory.server.core.factory.DirectoryServiceFactory;

import java.util.Locale;
import java.util.UUID;

/**
 * Custom ApacheDS {@link DirectoryServiceFactory} for running tests.
 * <p>
 * The {@link DefaultDirectoryServiceFactory} uses a storage directory based on the {@link CreateDS#name()}. Because
 * that name is static, running tests in parallel and using the same TMPDIR leads to test failures.
 */
public class ApacheDirectoryTestServiceFactory extends DefaultDirectoryServiceFactory {
    /**
     * This init method ensures that the directory service is using a unique name so a unique storage location is
     * used in TMPDIR.
     *
     * @see DefaultDirectoryServiceFactory#buildInstanceDirectory(String)
     * @param name the server instance name
     */
    @Override
    public void init(String name) throws Exception {
        super.init(String.format(Locale.ENGLISH, "%s-%s", name, UUID.randomUUID().toString()));
    }
}

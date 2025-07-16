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
package org.graylog2.shared.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

/**
 * Class loader wrapper that logs failed class and resource lookups.
 */
public class LoggingClassLoader extends ClassLoader {
    private static final Logger LOG = LoggerFactory.getLogger(LoggingClassLoader.class);

    public LoggingClassLoader(ClassLoader parent) {
        super(parent.getName(), parent);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        try {
            return super.loadClass(name, resolve);
        } catch (ClassNotFoundException e) {
            LOG.debug("Couldn't find class <{}> in class loader <{}>", name, getName());
            throw e;
        }
    }

    @Override
    public URL getResource(String name) {
        final var resource = super.getResource(name);
        if (resource == null) {
            LOG.debug("Couldn't get <{}> resource from class loader <{}>", name, getName());
        }
        return resource;
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        final var resourceAsStream = super.getResourceAsStream(name);
        if (resourceAsStream == null) {
            LOG.debug("Couldn't get <{}> resource as stream from class loader <{}>", name, getName());
        }
        return resourceAsStream;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        final var resources = super.getResources(name);
        if (!resources.hasMoreElements()) {
            LOG.debug("Couldn't get <{}> resources from class loader <{}>", name, getName());
        }
        return resources;
    }
}

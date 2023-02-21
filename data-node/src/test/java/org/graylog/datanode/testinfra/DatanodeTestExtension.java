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
package org.graylog.datanode.testinfra;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.io.IOException;

public class DatanodeTestExtension implements ParameterResolver, BeforeAllCallback, AfterAllCallback {

    private final DatanodeContainerizedBackend datanodeBackend;

    public DatanodeTestExtension() {
        this.datanodeBackend = new DatanodeContainerizedBackend();
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getParameterizedType().equals(DatanodeContainerizedBackend.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        if (parameterContext.getParameter().getParameterizedType().equals(DatanodeContainerizedBackend.class)) {
            return this.datanodeBackend;
        } else {
            throw new IllegalArgumentException("Unsupported parameter " + parameterContext.getParameter().getParameterizedType());
        }
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws IOException {
        //TODO: trigger packaging: MavenPackager
        datanodeBackend.start();
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        datanodeBackend.stop();
    }
}

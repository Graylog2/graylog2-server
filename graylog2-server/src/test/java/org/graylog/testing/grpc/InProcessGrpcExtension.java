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
package org.graylog.testing.grpc;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * JUnit 5 extension that injects an {@link InProcessGrpc} instance into test methods, lifecycle methods and
 * constructors.
 * <p>
 * The instance is scoped to the extension context it was first resolved in and closed when that context ends. When
 * resolved in a {@code @BeforeEach} or test method, all servers and channels are shut down after each test. When
 * resolved in a constructor or {@code @BeforeAll} method, they are shared by all tests in the class and shut down
 * after all tests have run.
 *
 * <pre>{@code
 * @ExtendWith(InProcessGrpcExtension.class)
 * class MyServiceTest {
 *     private ManagedChannel channel;
 *
 *     @BeforeEach
 *     void setUp(InProcessGrpc grpc) throws IOException {
 *         channel = grpc.start(new MyService(), new MyInterceptor());
 *     }
 * }
 * }</pre>
 */
public class InProcessGrpcExtension implements ParameterResolver {
    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(InProcessGrpcExtension.class);

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return InProcessGrpc.class.equals(parameterContext.getParameter().getType());
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        // The store closes AutoCloseable values when the extension context ends.
        return extensionContext.getStore(NAMESPACE)
                .computeIfAbsent(InProcessGrpc.class, key -> new InProcessGrpc(), InProcessGrpc.class);
    }
}

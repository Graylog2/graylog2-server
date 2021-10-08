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
package org.junit.jupiter.engine.descriptor;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.jupiter.engine.execution.JupiterEngineExecutionContext;
import org.junit.jupiter.engine.extension.MutableExtensionRegistry;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;
import org.junit.platform.engine.support.hierarchical.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

import static org.junit.jupiter.engine.descriptor.JupiterTestDescriptor.toExecutionMode;

public class ContainerMatrixEngineDescriptor extends EngineDescriptor implements Node<JupiterEngineExecutionContext> {
    private static final Logger LOG = LoggerFactory.getLogger(ContainerMatrixEngineDescriptor.class);

    private final JupiterConfiguration configuration;

    public ContainerMatrixEngineDescriptor(UniqueId uniqueId, String displayName, JupiterConfiguration configuration) {
        super(uniqueId, displayName);
        this.configuration = configuration;
    }

    public JupiterConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public Node.ExecutionMode getExecutionMode() {
        return toExecutionMode(configuration.getDefaultExecutionMode());
    }

    @Override
    public JupiterEngineExecutionContext prepare(JupiterEngineExecutionContext context) {
        MutableExtensionRegistry extensionRegistry = MutableExtensionRegistry.createRegistryWithDefaultExtensions(
                context.getConfiguration());
        EngineExecutionListener executionListener = context.getExecutionListener();
        ExtensionContext extensionContext = new ContainerMatrixExtensionContext(executionListener, this,
                context.getConfiguration());

        // @formatter:off
        return context.extend()
                .withExtensionRegistry(extensionRegistry)
                .withExtensionContext(extensionContext)
                .build();
        // @formatter:on
    }

    @Override
    public void cleanUp(JupiterEngineExecutionContext context) throws Exception {
        context.close();
    }

    public void clearChildren() {
        this.children.clear();
    }

    public void addChildren(Collection<? extends TestDescriptor> children) {
        this.children.addAll(children);
    }
}

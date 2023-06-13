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
package org.graylog2.cluster.preflight;

import com.google.common.eventbus.EventBus;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Optional;
import java.util.stream.Stream;

public class NodePreflightConfigBusEvents implements NodePreflightConfigService {

    public static final String DELEGATE_NAME = "impl";

    private final NodePreflightConfigService delegate;
    private final EventBus eventBus;

    @Inject
    public NodePreflightConfigBusEvents(@Named(DELEGATE_NAME) NodePreflightConfigService delegate, EventBus eventBus) {
        this.delegate = delegate;
        this.eventBus = eventBus;
    }

    @Override
    public void changeState(String nodeId, NodePreflightConfig.State state) {
        delegate.changeState(nodeId, state);
        eventBus.post(new NodePreflightStateChangeEvent(nodeId, state));
    }

    @Override
    public NodePreflightConfig save(NodePreflightConfig config) {
        final NodePreflightConfig saved = delegate.save(config);
        eventBus.post(new NodePreflightStateChangeEvent(config.nodeId(), config.state()));
        return saved;
    }

    @Override
    public NodePreflightConfig getPreflightConfigFor(String nodeId) {
        return delegate.getPreflightConfigFor(nodeId);
    }

    @Override
    public void writeCsr(String nodeId, String csr) {
        delegate.writeCsr(nodeId, csr);
    }

    @Override
    public void writeCert(String nodeId, String cert) {
        delegate.writeCert(nodeId, cert);
    }

    @Override
    public Optional<String> readCert(String nodeId) {
        return delegate.readCert(nodeId);
    }

    @Override
    public Stream<NodePreflightConfig> streamAll() {
        return delegate.streamAll();
    }

    @Override
    public int delete(String id) {
        return delegate.delete(id);
    }

    @Override
    public void deleteAll() {
        delegate.deleteAll();
    }
}

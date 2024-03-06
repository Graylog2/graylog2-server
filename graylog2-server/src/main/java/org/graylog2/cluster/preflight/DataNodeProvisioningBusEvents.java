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

import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class DataNodeProvisioningBusEvents implements DataNodeProvisioningService {

    public static final String DELEGATE_NAME = "impl";

    private final DataNodeProvisioningService delegate;
    private final EventBus eventBus;

    @Inject
    public DataNodeProvisioningBusEvents(@Named(DELEGATE_NAME) DataNodeProvisioningService delegate, EventBus eventBus) {
        this.delegate = delegate;
        this.eventBus = eventBus;
    }

    @Override
    public void changeState(String nodeId, DataNodeProvisioningConfig.State state) {
        delegate.changeState(nodeId, state);
        eventBus.post(new DataNodeProvisioningStateChangeEvent(nodeId, state));
    }

    @Override
    public DataNodeProvisioningConfig save(DataNodeProvisioningConfig config) {
        final DataNodeProvisioningConfig saved = delegate.save(config);
        eventBus.post(new DataNodeProvisioningStateChangeEvent(config.nodeId(), config.state()));
        return saved;
    }

    @Override
    public Optional<DataNodeProvisioningConfig> getPreflightConfigFor(String nodeId) {
        return delegate.getPreflightConfigFor(nodeId);
    }

    @Override
    public List<DataNodeProvisioningConfig> findAllNodesThatNeedAttention() {
        return delegate.findAllNodesThatNeedAttention();
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
    public Stream<DataNodeProvisioningConfig> streamAll() {
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

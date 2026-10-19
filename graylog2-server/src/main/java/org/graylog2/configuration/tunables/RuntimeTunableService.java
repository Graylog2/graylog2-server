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
package org.graylog2.configuration.tunables;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog2.cluster.ClusterConfigChangedEvent;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.rest.ValidationFailureException;
import org.graylog2.plugin.rest.ValidationResult;
import org.graylog2.plugin.system.NodeId;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import static org.graylog2.shared.utilities.StringUtils.f;

@Singleton
public class RuntimeTunableService {
    public record Value(Object value, boolean isDefault) {}

    private final ClusterConfigService clusterConfigService;
    private final TunableValidator validator;
    private final RuntimeTunableSecrets secrets;
    private final ClusterEventBus clusterEventBus;
    private final NodeId nodeId;

    @Inject
    public RuntimeTunableService(ClusterConfigService clusterConfigService,
                                 TunableValidator validator,
                                 RuntimeTunableSecrets secrets,
                                 ClusterEventBus clusterEventBus,
                                 NodeId nodeId) {
        this.clusterConfigService = clusterConfigService;
        this.validator = validator;
        this.secrets = secrets;
        this.clusterEventBus = clusterEventBus;
        this.nodeId = nodeId;
    }

    public Value read(RuntimeTunableDescriptor descriptor) {
        final Object stored = clusterConfigService.get(descriptor.type());
        return stored != null ? new Value(stored, false) : new Value(descriptor.defaults(), true);
    }

    public Object update(RuntimeTunableDescriptor descriptor, Object config) {
        final Object stored = clusterConfigService.get(descriptor.type());
        final Object prepared = secrets.resolve(descriptor, config, stored);
        final ValidationResult result = validator.validate(descriptor.type(), descriptor.schema(), prepared);
        if (result.failed()) {
            throw new ValidationFailureException(result, f("Invalid runtime tunable \"%s\"", descriptor.id()));
        }
        clusterConfigService.write(prepared);
        return prepared;
    }

    public void reset(RuntimeTunableDescriptor descriptor) {
        clusterConfigService.remove(descriptor.type());
        clusterEventBus.post(ClusterConfigChangedEvent.create(DateTime.now(DateTimeZone.UTC), nodeId.getNodeId(),
                descriptor.type().getCanonicalName()));
    }
}

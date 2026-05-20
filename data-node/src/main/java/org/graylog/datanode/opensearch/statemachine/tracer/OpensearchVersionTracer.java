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
package org.graylog.datanode.opensearch.statemachine.tracer;

import com.github.zafarkhaja.semver.Version;
import jakarta.inject.Inject;
import org.graylog.datanode.OpensearchDistribution;
import org.graylog.datanode.configuration.DatanodeConfiguration;
import org.graylog.datanode.opensearch.statemachine.OpensearchEvent;
import org.graylog.datanode.opensearch.statemachine.OpensearchState;
import org.graylog.datanode.process.statemachine.tracer.StateMachineTracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.Optional;

public class OpensearchVersionTracer implements StateMachineTracer<OpensearchState, OpensearchEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(OpensearchVersionTracer.class);

    private final DatanodeConfiguration configuration;

    @Inject
    public OpensearchVersionTracer(DatanodeConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void trigger(OpensearchEvent trigger) {
    }

    @Override
    public void transition(OpensearchEvent trigger, OpensearchState source, OpensearchState destination) {
        if(source != destination && destination == OpensearchState.AVAILABLE) {
            final OpensearchDistribution opensearchDistribution = configuration.opensearchDistribution();
            final String osVersion = opensearchDistribution.version();
            LOG.info(String.format("Confirmed Opensearch version %s", osVersion));

            final Version currentVersion = Version.parse(opensearchDistribution.version());

            if(!opensearchDistribution.otherCandidates().isEmpty()) {
                final Optional<OpensearchDistribution> newerVersion = opensearchDistribution.otherCandidates().stream()
                        .filter(candidate -> Version.parse(candidate.version()).isHigherThan(currentVersion))
                        .max(Comparator.comparing(d -> Version.parse(d.version())));
                newerVersion.ifPresent(candidate -> {
                    // TODO: trigger notification here!
                    LOG.warn("You are running outdated Opensearch version. Perform index migration to activate latest version {}", candidate.version());
                });
            }

        }
    }
}

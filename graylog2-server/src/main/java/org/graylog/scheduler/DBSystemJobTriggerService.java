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
package org.graylog.scheduler;

import com.github.joschi.jadconfig.util.Duration;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.graylog.scheduler.capabilities.SchedulerCapabilitiesService;
import org.graylog.scheduler.clock.JobSchedulerClock;
import org.graylog2.database.MongoCollections;
import org.graylog2.plugin.system.NodeId;

import static org.graylog.scheduler.JobSchedulerConfiguration.LOCK_EXPIRATION_DURATION;

/**
 * Service for managing system job triggers stored in the database. This service uses a different collection than
 * the {@link DBJobTriggerService}!
 */
public class DBSystemJobTriggerService extends DBJobTriggerService {
    private static final String COLLECTION_NAME = "scheduler_system_triggers";

    @Inject
    public DBSystemJobTriggerService(MongoCollections mongoCollections,
                                     NodeId nodeId,
                                     JobSchedulerClock clock,
                                     SchedulerCapabilitiesService schedulerCapabilitiesService,
                                     @Named(LOCK_EXPIRATION_DURATION) Duration lockExpirationDuration) {
        super(mongoCollections, COLLECTION_NAME, nodeId, clock, schedulerCapabilitiesService, lockExpirationDuration);
    }
}

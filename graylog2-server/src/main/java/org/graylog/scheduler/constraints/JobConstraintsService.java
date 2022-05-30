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
package org.graylog.scheduler.constraints;

import javax.inject.Inject;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builds the current Set of capabilities this node provides.
 * These will be matched against the constraints of JobTriggers
 */
public class JobConstraintsService {
    private final Set<JobConstraints> jobConstraints;

    @Inject
    public JobConstraintsService(Set<JobConstraints> jobConstraints) {
        this.jobConstraints = jobConstraints;
    }

    public Set<String> getJobCapabilities() {
        return jobConstraints.stream().flatMap(s -> s.getNodeCapabilities().stream()).collect(Collectors.toSet());
    }
}

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
package org.graylog2.contentpacks.constraints;

import com.google.common.collect.ImmutableSet;
import org.graylog2.contentpacks.model.constraints.Constraint;
import org.graylog2.contentpacks.model.constraints.ConstraintCheckResult;
import org.graylog2.contentpacks.model.constraints.GraylogVersionConstraint;
import org.graylog2.contentpacks.model.constraints.PluginVersionConstraint;
import org.junit.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class GraylogVersionConstraintCheckerTest {
    @Test
    public void checkConstraints() {
        final GraylogVersionConstraintChecker constraintChecker = new GraylogVersionConstraintChecker("1.0.0");

        final GraylogVersionConstraint graylogVersionConstraint = GraylogVersionConstraint.builder()
                .version("^1.0.0")
                .build();
        final PluginVersionConstraint pluginVersionConstraint = PluginVersionConstraint.builder()
                .pluginId("unique-id")
                .version("^1.0.0")
                .build();
        final ImmutableSet<Constraint> requiredConstraints = ImmutableSet.of(graylogVersionConstraint, pluginVersionConstraint);
        final Set<ConstraintCheckResult> result = constraintChecker.checkConstraints(requiredConstraints);
        assertThat(result.stream().allMatch(c -> c.fulfilled())).isTrue();
    }

    @Test
    public void checkConstraintsFails() {
        final GraylogVersionConstraintChecker constraintChecker = new GraylogVersionConstraintChecker("1.0.0");

        final GraylogVersionConstraint graylogVersionConstraint = GraylogVersionConstraint.builder()
                .version("^2.0.0")
                .build();
        final PluginVersionConstraint pluginVersionConstraint = PluginVersionConstraint.builder()
                .pluginId("unique-id")
                .version("^1.0.0")
                .build();
        final ImmutableSet<Constraint> requiredConstraints = ImmutableSet.of(graylogVersionConstraint, pluginVersionConstraint);
        final Set<ConstraintCheckResult> result = constraintChecker.checkConstraints(requiredConstraints);
        assertThat(result.stream().allMatch(c -> !c.fulfilled())).isTrue();
    }
}
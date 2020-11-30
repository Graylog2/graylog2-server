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
import com.vdurmont.semver4j.Requirement;
import com.vdurmont.semver4j.Semver;
import org.graylog2.contentpacks.model.constraints.Constraint;
import org.graylog2.contentpacks.model.constraints.ConstraintCheckResult;
import org.graylog2.contentpacks.model.constraints.PluginVersionConstraint;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.Version;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class PluginVersionConstraintChecker implements ConstraintChecker {
    private final Set<Semver> pluginVersions;

    @Inject
    public PluginVersionConstraintChecker(Set<PluginMetaData> pluginMetaData) {
        pluginVersions = pluginMetaData.stream()
                .map(PluginMetaData::getVersion)
                .map(Version::toString)
                .map(Semver::new)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Constraint> ensureConstraints(Collection<Constraint> requestedConstraints) {
        final ImmutableSet.Builder<Constraint> fulfilledConstraints = ImmutableSet.builder();
        for (Constraint constraint : requestedConstraints) {
            if (constraint instanceof PluginVersionConstraint) {
                final PluginVersionConstraint versionConstraint = (PluginVersionConstraint) constraint;
                final Requirement requiredVersion = versionConstraint.version();

                for (Semver pluginVersion : pluginVersions) {
                    if (requiredVersion.isSatisfiedBy(pluginVersion)) {
                        fulfilledConstraints.add(constraint);
                    }
                }
            }
        }
        return fulfilledConstraints.build();
    }

    @Override
    public Set<ConstraintCheckResult> checkConstraints(Collection<Constraint> requestedConstraints) {
        final ImmutableSet.Builder<ConstraintCheckResult> fulfilledConstraints = ImmutableSet.builder();
        for (Constraint constraint : requestedConstraints) {
            if (constraint instanceof PluginVersionConstraint) {
                final PluginVersionConstraint versionConstraint = (PluginVersionConstraint) constraint;
                final Requirement requiredVersion = versionConstraint.version();

                boolean result = false;
                for (Semver pluginVersion : pluginVersions) {
                    if (requiredVersion.isSatisfiedBy(pluginVersion)) {
                        result = true;
                    }
                }
                ConstraintCheckResult constraintCheckResult = ConstraintCheckResult.create(versionConstraint, result);
                fulfilledConstraints.add(constraintCheckResult);
            }
        }
        return fulfilledConstraints.build();
    }
}

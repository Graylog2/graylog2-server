/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.contentpacks.constraints;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.vdurmont.semver4j.Requirement;
import com.vdurmont.semver4j.Semver;
import org.graylog2.contentpacks.model.constraints.Constraint;
import org.graylog2.contentpacks.model.constraints.ConstraintCheckResult;
import org.graylog2.contentpacks.model.constraints.GraylogVersionConstraint;

import java.util.Collection;
import java.util.Set;

public class GraylogVersionConstraintChecker implements ConstraintChecker {
    private final Semver graylogVersion;

    public GraylogVersionConstraintChecker() {
        this(org.graylog2.plugin.Version.CURRENT_CLASSPATH.toString());
    }

    @VisibleForTesting
    GraylogVersionConstraintChecker(String graylogVersion) {
        this(new Semver(graylogVersion));
    }

    @VisibleForTesting
    GraylogVersionConstraintChecker(Semver graylogVersion) {
        this.graylogVersion = graylogVersion;
    }


    @Override
    public Set<Constraint> ensureConstraints(Collection<Constraint> requestedConstraints) {
        final ImmutableSet.Builder<Constraint> fulfilledConstraints = ImmutableSet.builder();
        for (Constraint constraint : requestedConstraints) {
            if (constraint instanceof GraylogVersionConstraint) {
                final GraylogVersionConstraint versionConstraint = (GraylogVersionConstraint) constraint;
                final Requirement requiredVersion = versionConstraint.version();
                if (requiredVersion.isSatisfiedBy(graylogVersion.toString())) {
                    fulfilledConstraints.add(constraint);
                }
            }
        }
        return fulfilledConstraints.build();
    }

    @Override
    public Set<ConstraintCheckResult> checkConstraints(Collection<Constraint> requestedConstraints) {
        final ImmutableSet.Builder<ConstraintCheckResult> fulfilledConstraints = ImmutableSet.builder();
        for (Constraint constraint : requestedConstraints) {
            if (constraint instanceof GraylogVersionConstraint) {
                final GraylogVersionConstraint versionConstraint = (GraylogVersionConstraint) constraint;
                final Requirement requiredVersion = versionConstraint.version();
                final ConstraintCheckResult constraintCheckResult = ConstraintCheckResult.create(versionConstraint,
                        requiredVersion.isSatisfiedBy(graylogVersion.toString()));
                fulfilledConstraints.add(constraintCheckResult);
            }
        }
        return fulfilledConstraints.build();
    }
}

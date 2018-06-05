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

import com.github.zafarkhaja.semver.Version;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import org.graylog2.contentpacks.model.constraints.Constraint;
import org.graylog2.contentpacks.model.constraints.GraylogVersionConstraint;

import java.util.Collection;
import java.util.Set;

import static java.util.Objects.requireNonNull;

public class GraylogVersionConstraintChecker implements ConstraintChecker {
    private final Version graylogVersion;

    public GraylogVersionConstraintChecker() {
        this(org.graylog2.plugin.Version.CURRENT_CLASSPATH.getVersion());
    }

    @VisibleForTesting
    GraylogVersionConstraintChecker(Version graylogVersion) {
        this.graylogVersion = requireNonNull(graylogVersion, "graylogVersion");
    }

    @Override
    public Set<Constraint> checkConstraints(Collection<Constraint> requestedConstraints) {
        final ImmutableSet.Builder<Constraint> fulfilledConstraints = ImmutableSet.builder();
        for (Constraint constraint : requestedConstraints) {
            if (constraint instanceof GraylogVersionConstraint) {
                final GraylogVersionConstraint versionConstraint = (GraylogVersionConstraint) constraint;
                final Version requiredVersion = versionConstraint.getVersion();

                // TODO: Is the version requirement strict enough?
                if (graylogVersion.greaterThanOrEqualTo(requiredVersion)) {
                    fulfilledConstraints.add(constraint);
                }
            }
        }
        return fulfilledConstraints.build();
    }
}

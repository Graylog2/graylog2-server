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

import com.google.common.collect.ImmutableSet;
import com.vdurmont.semver4j.Requirement;
import com.vdurmont.semver4j.Semver;
import org.graylog2.contentpacks.model.constraints.Constraint;
import org.graylog2.contentpacks.model.constraints.PluginVersionConstraint;
import org.graylog2.plugin.PluginMetaData;

import java.util.Collection;
import java.util.Set;

public abstract class AbstractPluginVersionConstraintChecker<T extends PluginMetaData> implements ConstraintChecker {
    private final Semver pluginVersion;

    protected AbstractPluginVersionConstraintChecker(T pluginMetaData) {
        this(pluginMetaData.getVersion().toString());
    }

    protected AbstractPluginVersionConstraintChecker(String pluginVersion) {
        this(new Semver(pluginVersion));
    }

    protected AbstractPluginVersionConstraintChecker(Semver pluginVersion) {
        this.pluginVersion = pluginVersion;
    }

    @Override
    public Set<Constraint> checkConstraints(Collection<Constraint> requestedConstraints) {
        final ImmutableSet.Builder<Constraint> fulfilledConstraints = ImmutableSet.builder();
        for (Constraint constraint : requestedConstraints) {
            if (constraint instanceof PluginVersionConstraint) {
                final PluginVersionConstraint versionConstraint = (PluginVersionConstraint) constraint;
                final Requirement requiredVersion = versionConstraint.version();

                if (requiredVersion.isSatisfiedBy(pluginVersion)) {
                    fulfilledConstraints.add(constraint);
                }
            }
        }
        return fulfilledConstraints.build();
    }
}

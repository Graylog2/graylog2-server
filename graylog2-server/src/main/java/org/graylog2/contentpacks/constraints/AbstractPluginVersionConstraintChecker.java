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
import com.google.common.collect.ImmutableSet;
import org.graylog2.contentpacks.model.constraints.Constraint;
import org.graylog2.contentpacks.model.constraints.PluginVersionConstraint;
import org.graylog2.plugin.PluginMetaData;

import java.util.Collection;
import java.util.Set;

public abstract class AbstractPluginVersionConstraintChecker<T extends PluginMetaData> implements ConstraintChecker {
    private final Version pluginVersion;

    protected AbstractPluginVersionConstraintChecker(T pluginMetaData) {
        this(pluginMetaData.getVersion().getVersion());
    }

    protected AbstractPluginVersionConstraintChecker(Version pluginVersion) {
        this.pluginVersion = pluginVersion;
    }

    @Override
    public Set<Constraint> checkConstraints(Collection<Constraint> requestedConstraints) {
        final ImmutableSet.Builder<Constraint> fulfilledConstraints = ImmutableSet.builder();
        for (Constraint constraint : requestedConstraints) {
            if (constraint instanceof PluginVersionConstraint) {
                final PluginVersionConstraint versionConstraint = (PluginVersionConstraint) constraint;
                final Version requiredVersion = versionConstraint.getVersion();

                // TODO: Is the version requirement strict enough?
                if (pluginVersion.greaterThanOrEqualTo(requiredVersion)) {
                    fulfilledConstraints.add(constraint);
                }
            }
        }
        return fulfilledConstraints.build();
    }
}

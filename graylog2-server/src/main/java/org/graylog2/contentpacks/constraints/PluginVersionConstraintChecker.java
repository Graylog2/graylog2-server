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
    public Set<Constraint> checkConstraints(Collection<Constraint> requestedConstraints) {
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
}

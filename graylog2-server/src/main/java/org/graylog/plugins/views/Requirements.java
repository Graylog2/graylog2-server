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
package org.graylog.plugins.views;

import com.google.common.collect.ForwardingMap;
import com.google.inject.assistedinject.Assisted;
import org.graylog.plugins.views.search.views.PluginMetadataSummary;

import javax.inject.Inject;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Requirements<O> extends ForwardingMap<String, PluginMetadataSummary> {
    private final Map<String, PluginMetadataSummary> requirements;
    private final O dto;

    @Inject
    public Requirements(Set<? extends Requirement<O>> requirements, @Assisted O dto) {
        this.dto = dto;
        this.requirements = requirements.stream()
                .map(requirement -> requirement.test(dto))
                .flatMap(s -> s.entrySet().stream())
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue, (entry1, entry2) -> entry1));
    }

    @Override
    protected Map<String, PluginMetadataSummary> delegate() {
        return this.requirements;
    }

    public O rebuildRequirements(Function<O, Map<String, PluginMetadataSummary>> getter,
                                 BiFunction<O, Map<String, PluginMetadataSummary>, O> setter) {
        final Map<String, PluginMetadataSummary> requirements = Stream.concat(
                getter.apply(dto).entrySet().stream(),
                this.entrySet().stream()
        )
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue, (entry1, entry2) -> entry1));
        return setter.apply(dto, requirements);

    }

    public interface Factory<O> {
        Requirements<O> create(O dto);
    }
}

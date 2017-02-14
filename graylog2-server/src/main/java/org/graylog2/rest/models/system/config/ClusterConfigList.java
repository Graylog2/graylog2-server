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
package org.graylog2.rest.models.system.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import org.graylog.autovalue.WithBeanGetter;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class ClusterConfigList {
    @JsonProperty
    public abstract int total();

    @JsonProperty
    public abstract Set<String> classes();

    public static ClusterConfigList create(Collection<String> classes) {
        return new AutoValue_ClusterConfigList(classes.size(), ImmutableSet.copyOf(classes));
    }

    public static ClusterConfigList createFromClass(Collection<Class<?>> classes) {
        return create(classes.stream().map(Class::getCanonicalName).collect(Collectors.toSet()));
    }
}

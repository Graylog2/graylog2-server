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
package org.graylog2.plugin.lookup;

import com.google.common.collect.Multimap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Optional;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = LookupCacheConfiguration.TYPE_FIELD,
        visible = true,
        defaultImpl = FallbackCacheConfig.class)
public interface LookupCacheConfiguration {
    String TYPE_FIELD = "type";

    @JsonProperty(TYPE_FIELD)
    String type();

    /**
     * <p>Override this method to check for logical errors in the configuration, such as missing
     * files, or invalid combinations of options. Prefer validation annotations for simple
     * per-property validations rules, such as min/max values, non-empty strings etc. </p>
     *
     * <p> By default the configuration has no extra validation errors (i.e. the result of this
     * method is {@link Optional#empty()}. </p>
     *
     * <p>Returning failing validations here <b>does not</b> prevent saving the configuration!</p>
     *
     * @return optionally map of property name to error messages
     */
    @JsonIgnore
    default Optional<Multimap<String, String>> validate() {
        return Optional.empty();
    }
}

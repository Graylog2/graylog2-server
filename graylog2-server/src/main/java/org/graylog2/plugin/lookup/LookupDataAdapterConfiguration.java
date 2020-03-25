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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.collect.Multimap;
import org.graylog2.lookup.adapters.LookupDataAdapterValidationContext;

import java.util.Optional;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = LookupDataAdapterConfiguration.TYPE_FIELD,
        visible = true,
        defaultImpl = FallbackAdapterConfig.class)
public interface LookupDataAdapterConfiguration {
    String TYPE_FIELD = "type";

    @JsonProperty(TYPE_FIELD)
    String type();

    /**
     * <p> Override this method to check for logical errors in the configuration, such as missing
     * files, or invalid combinations of options. Prefer validation annotations for simple
     * per-property validations rules, such as min/max values, non-empty strings etc. </p>
     *
     * <p> By default the configuration has no extra validation errors (i.e. the result of this
     * method is {@link Optional#empty()}. </p>
     *
     * <p>Returning failing validations here <b>does not</b> prevent saving the configuration!</p>
     *
     * <p>If your validation needs access to additional services, override
     * {@link #validate(LookupDataAdapterValidationContext)} instead. </p>
     *
     * @return optionally map of property name to error messages
     */
    @JsonIgnore
    default Optional<Multimap<String, String>> validate() {
        return Optional.empty();
    }

    /**
     * Same as {@link #validate()} but providing access to additional services via the given context object.
     * <p>If you override this message, don't also override {@link #validate()} as the calling code is not expected
     * to call both methods.</p>
     */
    @JsonIgnore
    default Optional<Multimap<String, String>> validate(LookupDataAdapterValidationContext validationContext) {
        return validate();
    }
}

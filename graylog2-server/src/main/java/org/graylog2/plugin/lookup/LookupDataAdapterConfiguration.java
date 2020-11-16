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

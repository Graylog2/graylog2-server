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
package org.graylog.plugins.views.search.rest.scriptingapi.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.ValidationException;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Values;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

//no column/row choice, assuming API does not care about visualization, and we can ignore it
public record Grouping(@JsonProperty("field") @Valid @NotBlank String fieldName,
                       @JsonProperty("limit") Optional<Integer> limit,
                       @JsonProperty("timeunit") Optional<String> timeunit,
                       @JsonProperty("scaling") Optional<Double> scaling) {

    public Grouping(String fieldName) {
        this(fieldName, Optional.of(Values.DEFAULT_LIMIT), Optional.empty(), Optional.empty());
    }

    public Grouping(@JsonProperty("field") @Valid @NotBlank String fieldName,
                    @JsonProperty("limit") Optional<Integer> limit,
                    @JsonProperty("timeunit") Optional<String> timeunit,
                    @JsonProperty("scaling") Optional<Double> scaling) {
        this.fieldName = fieldName;
        this.limit = limit.map(lim -> lim <= 0 ? Values.DEFAULT_LIMIT : lim);
        this.timeunit = timeunit;
        this.scaling = scaling;

        // only one of the three following parameters are allowed to be present
        final AtomicInteger attrCounter = new AtomicInteger();
        limit.ifPresent(l -> attrCounter.getAndIncrement());
        timeunit.ifPresent(t -> attrCounter.getAndIncrement());
        scaling.ifPresent(s -> attrCounter.getAndIncrement());
        if(attrCounter.get() > 1) {
            throw new ValidationException("Only one attribute out of 'limit', 'timeunit' or 'scaling' can be specified");
        }
    }

    public Grouping(@JsonProperty("field") @Valid @NotBlank String fieldName,
                    @JsonProperty("limit") int limit) {
        this(fieldName, Optional.of(limit), Optional.empty(), Optional.empty());
    }

    @Deprecated
    @Override
    public String fieldName() {
        return fieldName;
    }

    public RequestedField requestedField() {
        return RequestedField.parse(fieldName);
    }

}

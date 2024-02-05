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
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.NoClass;
import com.github.rvesse.airline.annotations.restrictions.NotBlank;
import org.apache.commons.lang.StringUtils;
import org.graylog.plugins.views.search.searchtypes.pivot.SortSpec;

import jakarta.validation.Valid;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

//TODO: move back to record if this one got solved: https://github.com/FasterXML/jackson-databind/issues/3342
public class Metric implements Sortable {

    @JsonProperty("field")
    private String fieldName;

    @JsonProperty("function")
    @NotBlank
    private @Valid String functionName;

    @JsonProperty("sort")
    private SortSpec.Direction sort;

    @JsonProperty("configuration")
    @JsonTypeInfo(include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
                  use = JsonTypeInfo.Id.NAME,
                  property = "function",
                  defaultImpl = NoClass.class)
    @JsonSubTypes({
            @JsonSubTypes.Type(name = "percentile", value = PercentileConfiguration.class),
            @JsonSubTypes.Type(name = "percentage", value = PercentageConfiguration.class)
    })
    private MetricConfiguration configuration;

    public Metric(@JsonProperty("function") String functionName, @JsonProperty("field") String fieldName,
                  @JsonProperty("sort") SortSpec.Direction sort,
                  @JsonProperty("configuration") MetricConfiguration configuration) {
        this.fieldName = fieldName;
        this.functionName = functionName;
        this.sort = sort;
        this.configuration = configuration;
    }


    public Metric(final String functionName, final String fieldName) {
        this(functionName, fieldName, null, null);
    }

    /**
     * Creates a new Metric from its string representation
     *
     * @param metricString String representation in the form of "function:field", i.e. "avg:took_ms" or "latest:source" (you can ommit field for count function : "count" or "count:")
     * @return new Metric, or null if metricString input string is blank
     */
    public static Optional<Metric> fromStringRepresentation(final String metricString) {
        if (StringUtils.isBlank(metricString)) {
            return Optional.empty();
        }
        final String[] split = metricString.split(":");
        final String functionName = split[0];
        final String fieldName = split.length > 1 ? split[1] : null;
        return Optional.of(new Metric(functionName, fieldName));
    }

    public String fieldName() {
        return fieldName;
    }

    public String functionName() {
        return Optional.ofNullable(functionName).map(fn -> fn.toLowerCase(Locale.ROOT)).orElse(null);
    }

    @Override
    public SortSpec.Direction sort() {
        return sort;
    }

    public MetricConfiguration configuration() {
        return configuration;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (Metric) obj;
        return Objects.equals(this.fieldName, that.fieldName) &&
                Objects.equals(this.functionName, that.functionName) &&
                Objects.equals(this.sort, that.sort) &&
                Objects.equals(this.configuration, that.configuration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fieldName, functionName, sort, configuration);
    }

    @Override
    public String toString() {
        return "Metric[" +
                "fieldName=" + fieldName + ", " +
                "functionName=" + functionName + ", " +
                "sort=" + sort + ", " +
                "configuration=" + configuration + ']';
    }

}

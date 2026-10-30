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
package org.graylog2.configuration.managed;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Describes a configuration parameter which can be read and changed through the API.
 * <p>
 * Registering a parameter is what exposes it: parameters without a descriptor can neither be read nor changed, no
 * matter whether a configuration bean declares them. Descriptors are plain data, so parameters of a data node can be
 * described here without the data node classes being available.
 *
 * @param order Position of the parameter within its category, lower values first
 * @param units The units the value can be given in, mapping the suffix appended to the amount to the label the user
 *              interface shows. Empty when the value has no unit, and in the order the options are offered in.
 */
public record ConfigurationParameter(@JsonProperty("node_type") NodeType nodeType,
                                     @JsonProperty("name") String name,
                                     @JsonProperty("label") String label,
                                     @JsonProperty("description") String description,
                                     @JsonProperty("category") String category,
                                     @JsonProperty("order") int order,
                                     @JsonProperty("type") ParameterType type,
                                     @JsonProperty("units") Map<String, String> units,
                                     @JsonProperty("constraints") ParameterConstraints constraints,
                                     @JsonProperty("requires_restart") boolean requiresRestart,
                                     @JsonProperty("backing") ParameterBacking backing,
                                     @JsonProperty("scope") ParameterScope scope) {

    public ConfigurationParameter {
        Objects.requireNonNull(nodeType, "nodeType");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(backing, "backing");
        Objects.requireNonNull(scope, "scope");
        description = description == null ? "" : description;
        category = category == null ? "" : category;
        // Map#copyOf doesn't keep the iteration order, which decides the order of the unit options
        units = units == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(units));
        constraints = constraints == null ? ParameterConstraints.none() : constraints;
    }

    /**
     * Checks a raw value against the type and the constraints of this parameter.
     *
     * @return a message describing why the value isn't accepted, empty if it is
     */
    public Optional<String> validate(String value) {
        if (value == null || value.isBlank()) {
            return Optional.of(f("Parameter <%s> requires a value.", name));
        }
        if (type == ParameterType.LIST) {
            final String[] elements = value.split(",", -1);
            return Arrays.stream(elements)
                    .map(String::trim)
                    .map(this::validateSingleValue)
                    .flatMap(Optional::stream)
                    .findFirst();
        }
        return validateSingleValue(value.trim());
    }

    private Optional<String> validateSingleValue(String value) {
        if (value.isEmpty()) {
            return Optional.of(f("Parameter <%s> must not contain empty values.", name));
        }
        if (!constraints.values().isEmpty() && !constraints.values().containsKey(value)) {
            return Optional.of(f("Value <%s> of parameter <%s> is not one of %s.",
                    value, name, constraints.values().keySet()));
        }
        if (constraints.pattern() != null && !Pattern.matches(constraints.pattern(), value)) {
            return Optional.of(f("Value <%s> of parameter <%s> doesn't match <%s>.",
                    value, name, constraints.pattern()));
        }
        if (type == ParameterType.INTEGER) {
            return validateInteger(value);
        }
        if (type == ParameterType.BOOLEAN && !"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
            return Optional.of(f("Value <%s> of parameter <%s> is not a boolean.", value, name));
        }
        return Optional.empty();
    }

    private Optional<String> validateInteger(String value) {
        final long number;
        try {
            number = Long.parseLong(value);
        } catch (NumberFormatException e) {
            return Optional.of(f("Value <%s> of parameter <%s> is not a number.", value, name));
        }
        if (constraints.min() != null && number < constraints.min()) {
            return Optional.of(f("Value <%s> of parameter <%s> is smaller than <%s>.", value, name, constraints.min()));
        }
        if (constraints.max() != null && number > constraints.max()) {
            return Optional.of(f("Value <%s> of parameter <%s> is larger than <%s>.", value, name, constraints.max()));
        }
        return Optional.empty();
    }

    public static Builder builder(NodeType nodeType, String name, String label) {
        return new Builder(nodeType, name, label);
    }

    public static final class Builder {
        private final NodeType nodeType;
        private final String name;
        private final String label;
        private String description = "";
        private String category = "";
        private int order = 100;
        private ParameterType type = ParameterType.STRING;
        private Map<String, String> units = Map.of();
        private ParameterConstraints constraints = ParameterConstraints.none();
        private boolean requiresRestart = true;
        private ParameterBacking backing = ParameterBacking.CONFIG_FILE;
        private ParameterScope scope = ParameterScope.NODE;

        private Builder(NodeType nodeType, String name, String label) {
            this.nodeType = nodeType;
            this.name = name;
            this.label = label;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder order(int order) {
            this.order = order;
            return this;
        }

        public Builder type(ParameterType type) {
            this.type = type;
            return this;
        }

        /**
         * The units the value can be given in, mapping each suffix to its label. Empty when the value has no unit.
         */
        public Builder units(Map<String, String> units) {
            this.units = units;
            return this;
        }

        public Builder constraints(ParameterConstraints constraints) {
            this.constraints = constraints;
            return this;
        }

        public Builder requiresRestart(boolean requiresRestart) {
            this.requiresRestart = requiresRestart;
            return this;
        }

        public Builder backing(ParameterBacking backing) {
            this.backing = backing;
            return this;
        }

        public Builder scope(ParameterScope scope) {
            this.scope = scope;
            return this;
        }

        public ConfigurationParameter build() {
            return new ConfigurationParameter(nodeType, name, label, description, category, order, type, units,
                    constraints, requiresRestart, backing, scope);
        }
    }
}

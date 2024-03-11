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
package org.graylog.plugins.views.storage.migration.state.machine;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.PROTECTED_AND_PUBLIC)
public class MigrationStateMachineContext {

    public static final String AUTH_TOKEN_KEY = "authToken";

    public static final String KEY_MIGRATION_ID = "migrationID";

    @JsonProperty
    protected MigrationStep currentStep;
    @JsonProperty
    protected Map<MigrationStep, Map<String, Object>> actionArguments;
    @JsonProperty
    protected Map<String, Object> extendedState;

    @JsonIgnore
    protected Object response;

    public MigrationStateMachineContext() {
        this.actionArguments = new HashMap<>();
        this.extendedState = new HashMap<>();
    }

    public void setCurrentStep(MigrationStep currentStep) {
        this.currentStep = currentStep;
    }

    public <T> T getActionArgument(String name, Class<T> type) {
        Map<String, Object> args = this.actionArguments.get(currentStep);
        if (Objects.isNull(args)) {
            throw new IllegalArgumentException("Missing arguments for step " + currentStep);
        }
        if (!args.containsKey(name)) {
            throw new IllegalArgumentException("Missing argument " + name + " for step " + currentStep);
        }
        Object arg = args.get(name);
        if (!type.isInstance(arg)) {
            throw new IllegalArgumentException("Argument " + name + " must be of type " + type);
        }
        return (T) arg;
    }

    public <T> Optional<T> getActionArgumentOpt(String name, Class<T> type) {
        Map<String, Object> args = this.actionArguments.get(currentStep);
        return Optional.ofNullable(args)
                .map(arg -> arg.get(name))
                .map(arg -> {
                    if (!type.isInstance(arg)) {
                        throw new IllegalArgumentException("Argument " + name + " must be of type " + type);
                    }
                    return (T) arg;
                });
    }

    public void addActionArguments(MigrationStep step, Map<String, Object> args) {
        this.actionArguments.put(step, args);
    }

    public void addExtendedState(String key, Object value) {
        this.extendedState.put(key, value);
    }

    public Object getExtendedState(String key) {
        return this.extendedState.get(key);
    }

    public <T> Optional<T> getExtendedState(String name, Class<T> type) {
        if (!this.extendedState.containsKey(name)) {
            return Optional.empty();
        }
        Object value = this.extendedState.get(name);
        if (!type.isInstance(value)) {
            if (value instanceof LinkedHashMap<?, ?> map) {
                try {
                    return Optional.of(new ObjectMapper().convertValue(map, type));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Argument " + name + " must be of type " + type);
                }
            }
            throw new IllegalArgumentException("Argument " + name + " must be of type " + type);
        }
        return Optional.of((T) value);
    }

    public void setResponse(Object response) {
        this.response = response;
    }

    public Object getResponse() {
        return response;
    }
}

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
package org.graylog.plugins.views.storage.migration.state.actions;

import org.graylog.plugins.views.storage.migration.state.machine.MigrationState;
import org.graylog.plugins.views.storage.migration.state.machine.MigrationStep;
import org.graylog.plugins.views.storage.migration.state.rest.CurrentStateInformation;

import java.util.List;
import java.util.Map;

public class MigrationActionContext {
    private final Map<String, Object> args;
    private final MigrationState previousState;
    private final MigrationStep requestedStep;

    // mutable results
    private CurrentStateInformation resultState;
    private List<String> errors;

    public MigrationActionContext(Map<String, Object> args, MigrationState previousState, MigrationStep requestedStep) {
        this.args = args;
        this.previousState = previousState;
        this.requestedStep = requestedStep;
    }

    public Map<String, Object> getArgs() {
        return args;
    }

    public MigrationState getPreviousState() {
        return previousState;
    }

    public MigrationStep getRequestedStep() {
        return requestedStep;
    }

    public CurrentStateInformation getResultState() {
        return resultState;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setResultState(CurrentStateInformation resultState) {
        this.resultState = resultState;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
}

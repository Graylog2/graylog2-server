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
package org.graylog.events.processor;

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.documentation.Documentation;
import com.github.joschi.jadconfig.documentation.DocumentationSection;
import com.github.joschi.jadconfig.validators.PositiveIntegerValidator;

@DocumentationSection(heading = "Event Definition", description = "")
public class EventDefinitionConfiguration {

    @Documentation("""
            Maximum value that can be set for an event limit.
            Default: 1000
            """)
    @Parameter(value = "event_definition_max_event_limit", validators = PositiveIntegerValidator.class)
    private int maxEventLimit = 1000;

    @Documentation("""
            Enforce strict format validation on tactic/technique IDs assigned to event definitions.
            When enabled, tactic IDs must match the pattern TA followed by 4 digits (e.g. TA0004) and
            technique IDs must match T followed by 4 digits with an optional 3-digit sub-technique
            suffix (e.g. T1021 or T1021.006). Disable as an emergency override if a new ID format is
            introduced before Graylog ships an updated validator.
            Default: true
            """)
    @Parameter(value = "event_definition_tactics_techniques_validation_enabled")
    private boolean tacticsTechniquesValidationEnabled = true;

    public int getMaxEventLimit() {
        return maxEventLimit;
    }

    public boolean isTacticsTechniquesValidationEnabled() {
        return tacticsTechniquesValidationEnabled;
    }
}

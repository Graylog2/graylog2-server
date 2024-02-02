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
package org.graylog.datanode;

import io.restassured.response.ValidatableResponse;
import org.graylog.plugins.views.storage.migration.state.machine.MigrationState;
import org.graylog.plugins.views.storage.migration.state.machine.MigrationStep;
import org.graylog.shaded.kafka09.joptsimple.internal.Strings;
import org.hamcrest.Matchers;

import java.util.Map;

public interface MigrationITTools {
    default String request(final MigrationStep step) {
        return """
                {
                    "step": "%s"
                }
                """.formatted(step);
    }

    // TODO: replace with jackson!
    default String format(final Map<String, Object> args) {
        return Strings.join(args.entrySet().stream().map((entry) -> "\"" + entry.getKey() + "\": \"" + entry.getValue() + "\"\n").toList(), ",");
    }

    default String request(final MigrationStep step, final Map<String, Object> args) {
        return """
                {
                    "step": "%s",
                    "args": {
                        %s
                      }
                }
                """.formatted(step, format(args));
    }

    default void verify(final ValidatableResponse response, final MigrationState newState, final MigrationStep... steps) {
        response.assertThat().body("state", Matchers.equalTo(newState.name()));
        response.assertThat().body("next_steps", Matchers.hasSize(steps.length));
        for (int i = 0; i < steps.length; i++) {
            response.assertThat().body("next_steps[" + i + "]", Matchers.equalTo(steps[i].name()));
        }
    }
}

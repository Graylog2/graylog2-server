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

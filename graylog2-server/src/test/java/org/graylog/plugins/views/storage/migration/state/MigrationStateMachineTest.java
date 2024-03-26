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
package org.graylog.plugins.views.storage.migration.state;

import org.assertj.core.api.Assertions;
import org.graylog.plugins.views.storage.migration.state.machine.MigrationActionsAdapter;
import org.graylog.plugins.views.storage.migration.state.machine.MigrationState;
import org.graylog.plugins.views.storage.migration.state.machine.MigrationStateMachine;
import org.graylog.plugins.views.storage.migration.state.machine.MigrationStateMachineProvider;
import org.graylog.plugins.views.storage.migration.state.machine.MigrationStep;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;


class MigrationStateMachineTest {

    private static final Logger LOG = LoggerFactory.getLogger(MigrationStateMachineTest.class);

    @Test
    void testPersistence() {
        final InMemoryStateMachinePersistence persistence = new InMemoryStateMachinePersistence();

        final MigrationActionsAdapter migrationActions = new MigrationActionsAdapter() {};

        final MigrationStateMachine migrationStateMachine = new MigrationStateMachineProvider(persistence, migrationActions).get();
        migrationStateMachine.trigger(MigrationStep.SELECT_MIGRATION, Collections.emptyMap());
        migrationStateMachine.trigger(MigrationStep.SHOW_CA_CREATION, Collections.emptyMap());


        Assertions.assertThat(persistence.getConfiguration())
                .isPresent()
                .hasValueSatisfying(configuration -> {
                    Assertions.assertThat(configuration.currentState()).isEqualTo(MigrationState.CA_CREATION_PAGE);
                });
    }

    @Test
    void testReset() {
        final InMemoryStateMachinePersistence persistence = new InMemoryStateMachinePersistence();
        final MigrationStateMachineProvider provider = new MigrationStateMachineProvider(persistence, new MigrationActionsAdapter());
        final MigrationStateMachine sm = provider.get();
        sm.trigger(MigrationStep.SELECT_MIGRATION, Collections.emptyMap());
        sm.trigger(MigrationStep.SHOW_CA_CREATION, Collections.emptyMap());

        Assertions.assertThat(sm.getState()).isEqualTo(MigrationState.CA_CREATION_PAGE);

        sm.reset();

        Assertions.assertThat(sm.getState()).isEqualTo(MigrationState.NEW);

    }

    @Test
    void testSerialization() {
        final MigrationStateMachine migrationStateMachine = new MigrationStateMachineProvider(new InMemoryStateMachinePersistence(), new MigrationActionsAdapter()).get();
        final String serialized = migrationStateMachine.serialize();
        Assertions.assertThat(serialized).isNotEmpty().startsWith("digraph G {");
        final String fragment = URLEncoder.encode(serialized, StandardCharsets.UTF_8).replace("+", "%20");
        LOG.info("Render state machine on " + "https://dreampuf.github.io/GraphvizOnline/#" + fragment);
    }
}

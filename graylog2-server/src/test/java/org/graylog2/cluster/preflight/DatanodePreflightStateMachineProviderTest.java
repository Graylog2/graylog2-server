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
package org.graylog2.cluster.preflight;

import com.github.oxo42.stateless4j.StateMachine;
import org.assertj.core.api.Assertions;
import org.graylog2.plugin.system.SimpleNodeId;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

class DatanodePreflightStateMachineProviderTest {


    @Test
    void testStateMachine() throws IOException {

        final DatanodeProvisioningActions preflightActions = Mockito.mock(DatanodeProvisioningActions.class);
        final DatanodePreflightStateMachineProvider provider = new DatanodePreflightStateMachineProvider(new SimpleNodeId("5ca1ab1e-0000-4000-a000-000000000000"), new InMemoryDatanodePreflightStateService(), preflightActions);
        final StateMachine<DataNodeProvisioningConfig.State, DatanodeProvisioningEvent> stateMachine = provider.get();


        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        stateMachine.configuration().generateDotFileInto(os, true);
        System.out.println(os.toString(StandardCharsets.UTF_8));

        Assertions.assertThat(stateMachine.getState()).isEqualTo(DataNodeProvisioningConfig.State.UNCONFIGURED);

        stateMachine.fire(DatanodeProvisioningEvent.CREATE_PRIVATE_KEY);

        Mockito.verify(preflightActions, Mockito.times(1)).generateAndStorePrivateKey();
        Mockito.verify(preflightActions, Mockito.times(1)).generateCsrEvent();


        final ArgumentCaptor<String> certificateCaptor = ArgumentCaptor.forClass(String.class);
        stateMachine.fire(DatanodePreflightStateMachineProvider.TRIGGER_CERTIFICATE_RECEIVED, "!this is cert!");

        Mockito.verify(preflightActions, Mockito.times(1)).onCertificateReceivedEvent(certificateCaptor.capture());

        Assertions.assertThat(certificateCaptor.getValue()).isEqualTo("!this is cert!");

        Assertions.assertThat(stateMachine.getState()).isEqualTo(DataNodeProvisioningConfig.State.STARTUP_PREPARED);

        stateMachine.fire(DatanodeProvisioningEvent.STARTUP_REQUESTED);
        stateMachine.fire(DatanodeProvisioningEvent.CONNECTING_SUCCEEDED);

        Assertions.assertThat(stateMachine.getState()).isEqualTo(DataNodeProvisioningConfig.State.CONNECTED);

    }

    private static class InMemoryDatanodePreflightStateService implements DatanodePreflightStateService {

        private DataNodeProvisioningConfig.State state;

        @Override
        public Optional<DataNodeProvisioningConfig> getPreflightConfigFor(String nodeId) {
            return Optional.ofNullable(state).map(s -> DataNodeProvisioningConfig.builder().nodeId(nodeId).state(s).build());
        }

        @Override
        public DataNodeProvisioningConfig save(DataNodeProvisioningConfig config) {
            this.state = config.state();
            return config;
        }

        @Override
        public void changeState(String nodeId, DataNodeProvisioningConfig.State state) {
            this.state = state;
        }
    }
}

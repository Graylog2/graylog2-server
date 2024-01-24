package org.graylog.datanode.process;

import com.google.inject.Provider;
import jakarta.inject.Inject;

public class ProcessStateMachineProvider implements Provider<ProcessStateMachine> {
    private final ProcessStateMachine processStateMachine;

    @Inject
    public ProcessStateMachineProvider() {
        this.processStateMachine = ProcessStateMachine.createNew();
    }

    @Override
    public ProcessStateMachine get() {
        return processStateMachine;
    }
}

package org.graylog2.inputs;

import com.google.common.collect.Lists;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.shared.inputs.NoSuchInputTypeException;
import org.graylog2.shared.inputs.PersistedInputs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Iterator;
import java.util.List;

public class PersistedInputsImpl implements PersistedInputs {
    private static final Logger LOG = LoggerFactory.getLogger(PersistedInputsImpl.class);
    private final InputService inputService;
    private final ServerStatus serverStatus;

    @Inject
    public PersistedInputsImpl(InputService inputService, ServerStatus serverStatus) {
        this.inputService = inputService;
        this.serverStatus = serverStatus;
    }

    @Override
    public Iterator<MessageInput> iterator() {
        List<MessageInput> result = Lists.newArrayList();

        for (Input io : inputService.allOfThisNode(serverStatus.getNodeId().toString())) {
            try {
                final MessageInput input = inputService.getMessageInput(io);
                result.add(input);
            } catch (NoSuchInputTypeException e) {
                LOG.warn("Cannot launch persisted input. No such type [{}].", io.getType());
            } catch (Throwable e) {
                LOG.warn("Cannot launch persisted input. Exception caught: ", e);
            }
        }

        return result.iterator();
    }
}

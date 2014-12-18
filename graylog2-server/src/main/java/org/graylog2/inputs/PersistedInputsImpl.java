/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.inputs;

import com.google.common.collect.Lists;
import org.graylog2.database.NotFoundException;
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
                LOG.warn("Cannot instantiate persisted input. No such type [{}].", io.getType());
            } catch (Throwable e) {
                LOG.warn("Cannot instantiate persisted input. Exception caught: ", e);
            }
        }

        return result.iterator();
    }

    @Override
    public MessageInput get(String id) {
        try {
            return inputService.getMessageInput(inputService.find(id));
        } catch (NoSuchInputTypeException e) {
            LOG.warn("Cannot instantiate persisted input: ", e);
        } catch (NotFoundException e) {
            LOG.warn("Cannot find persisted Input with id {}", id);
        }

        return null;
    }

    @Override
    public boolean add(MessageInput e) {
        return false;
    }

    @Override
    public boolean remove(Object o) {
        return false;
    }
}

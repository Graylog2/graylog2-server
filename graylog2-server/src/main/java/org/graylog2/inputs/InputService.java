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

import org.graylog2.cluster.Node;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.PersistedService;
import org.graylog2.database.ValidationException;
import org.graylog2.plugin.inputs.Extractor;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.shared.inputs.NoSuchInputTypeException;

import java.util.List;
import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public interface InputService extends PersistedService {
    List<Input> allOfThisNode(String nodeId);

    List<Input> allOfRadio(Node radio);

    Input create(Map<String, Object> fields);

    Input find(String id) throws NotFoundException;

    Input findForThisNode(String nodeId, String id) throws NotFoundException;
    Input findForThisRadio(String radioId, String id) throws NotFoundException;
    Input findForThisNodeOrGlobal(String nodeId, String id) throws NotFoundException;
    Input findForThisRadioOrGlobal(String radioId, String id) throws NotFoundException;

    void addExtractor(Input input, Extractor extractor) throws ValidationException;

    void addStaticField(Input input, String key, String value) throws ValidationException;

    List<Extractor> getExtractors(Input input);

    void removeExtractor(Input input, String extractorId);

    void removeStaticField(Input input, String key);

    MessageInput buildMessageInput(Input io) throws NoSuchInputTypeException;
    MessageInput getMessageInput(Input io) throws NoSuchInputTypeException;
}

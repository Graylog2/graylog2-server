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
package org.graylog2.shared.inputs;

import com.google.common.collect.Maps;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.shared.bindings.InstantiationService;

import javax.inject.Inject;
import java.util.Map;
import java.util.Set;

public class MessageInputFactory {
    private final InstantiationService instantiationService;
    private final Set<Class<? extends MessageInput>> implClasses;

    @Inject
    public MessageInputFactory(InstantiationService instantiationService,
                               Set<Class<? extends MessageInput>> implClasses) {
        this.instantiationService = instantiationService;
        this.implClasses = implClasses;
    }

    public MessageInput create(String type) throws NoSuchInputTypeException {
        try {
            for (Class<? extends MessageInput> implClass : implClasses)
                if (implClass.getCanonicalName().equals(type))
                    return instantiationService.getInstance(implClass);
        } catch (Exception e) {
            throw new RuntimeException("Could not create input of type <" + type + ">", e);
        }
        throw new NoSuchInputTypeException("There is no input of type <" + type + "> registered.");
    }

    public Map<String, String> getAvailableInputs() {
        Map<String, String> result = Maps.newHashMap();
        for (Class<? extends MessageInput> implClass : implClasses) {
            MessageInput instance = instantiationService.getInstance(implClass);
            result.put(implClass.getCanonicalName(), instance.getName());
        }
        return result;
    }
}
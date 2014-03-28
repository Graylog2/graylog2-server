/*
 * Copyright 2012-2014 TORCH GmbH
 *
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

import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.shared.bindings.InstantiationService;

import javax.inject.Inject;

public class MessageInputFactory {
    private final InstantiationService instantiationService;

    @Inject
    public MessageInputFactory(InstantiationService instantiationService) {
        this.instantiationService = instantiationService;
    }

    public MessageInput create(String type) throws NoSuchInputTypeException {
        try {
            final ClassLoader classLoader = lookupClassLoader(type);
            if (classLoader == null) {
                throw new NoSuchInputTypeException("There is no classloader to load input of type <" + type + ">.");
            }
            Class c = Class.forName(type, true, classLoader);
            return (MessageInput) instantiationService.getInstance(c);
        } catch (ClassNotFoundException e) {
            throw new NoSuchInputTypeException("There is no input of type <" + type + "> registered.");
        } catch (Exception e) {
            throw new RuntimeException("Could not create input of type <" + type + ">", e);
        }
    }

    public ClassLoader lookupClassLoader(String type) {
        return InputRegistry.classLoaders.get(type);
    }
}
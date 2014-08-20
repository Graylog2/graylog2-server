/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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
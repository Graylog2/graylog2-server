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

package org.graylog2.plugin;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import org.graylog2.plugin.filters.MessageFilter;
import org.graylog2.plugin.inputs.MessageInput;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public abstract class PluginModule extends AbstractModule {
    protected void addMessageInput(Class<? extends MessageInput> messageInputClass) {
        TypeLiteral<Class<? extends MessageInput>> typeLiteral = new TypeLiteral<Class<? extends MessageInput>>(){};
        Multibinder<Class<? extends MessageInput>> messageInputs = Multibinder.newSetBinder(binder(), typeLiteral);
        messageInputs.addBinding().toInstance(messageInputClass);
    }

    protected void addMessageFilter(Class<? extends MessageFilter> messageFilterClass) {
        Multibinder<MessageFilter> messageInputs = Multibinder.newSetBinder(binder(), MessageFilter.class);
        messageInputs.addBinding().to(messageFilterClass);
    }
}

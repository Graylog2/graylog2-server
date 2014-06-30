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

import com.google.common.util.concurrent.Service;
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import org.graylog2.plugin.alarms.callbacks.AlarmCallback;
import org.graylog2.plugin.filters.MessageFilter;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.periodical.Periodical;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public abstract class PluginModule extends AbstractModule {
    protected void registerPlugin(Class<? extends PluginMetaData> pluginMetaData) {
        Multibinder<PluginMetaData> pluginMetaDataMultibinder = Multibinder.newSetBinder(binder(), PluginMetaData.class);
        pluginMetaDataMultibinder.addBinding().to(pluginMetaData);
    }
    protected void addMessageInput(Class<? extends MessageInput> messageInputClass) {
        TypeLiteral<Class<? extends MessageInput>> typeLiteral = new TypeLiteral<Class<? extends MessageInput>>(){};
        Multibinder<Class<? extends MessageInput>> messageInputs = Multibinder.newSetBinder(binder(), typeLiteral);
        messageInputs.addBinding().toInstance(messageInputClass);
    }

    protected void addMessageFilter(Class<? extends MessageFilter> messageFilterClass) {
        Multibinder<MessageFilter> messageInputs = Multibinder.newSetBinder(binder(), MessageFilter.class);
        messageInputs.addBinding().to(messageFilterClass);
    }

    protected void addPeriodical(Class<? extends Periodical> periodicalClass) {
        Multibinder<Periodical> periodicalBinder = Multibinder.newSetBinder(binder(), Periodical.class);
        periodicalBinder.addBinding().to(periodicalClass);
    }

    protected void addAlarmCallback(Class<? extends AlarmCallback> alarmCallbackClass) {
        Multibinder<AlarmCallback> alarmCallbackInstanceBinder = Multibinder.newSetBinder(binder(), AlarmCallback.class);
        alarmCallbackInstanceBinder.addBinding().to(alarmCallbackClass);

        TypeLiteral<Class<? extends AlarmCallback>> type = new TypeLiteral<Class<? extends AlarmCallback>>(){};
        Multibinder<Class<? extends AlarmCallback>> alarmCallbackBinder = Multibinder.newSetBinder(binder(), type);
        alarmCallbackBinder.addBinding().toInstance(alarmCallbackClass);
    }

    protected void addInitializer(Class<? extends Service> initializerClass) {
        Multibinder<Service> serviceBinder = Multibinder.newSetBinder(binder(), Service.class);
        serviceBinder.addBinding().to(initializerClass);
    }

    protected void addMessageOutput(Class<? extends MessageOutput> messageOutputClass) {
        TypeLiteral<Class<? extends MessageOutput>> typeLiteral = new TypeLiteral<Class<? extends MessageOutput>>(){};
        Multibinder<Class<? extends MessageOutput>> messageOutputs = Multibinder.newSetBinder(binder(), typeLiteral);
        messageOutputs.addBinding().toInstance(messageOutputClass);
    }
}

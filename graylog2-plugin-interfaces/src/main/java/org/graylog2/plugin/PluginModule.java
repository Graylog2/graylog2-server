/**
 * The MIT License
 * Copyright (c) 2012 Graylog, Inc.
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
package org.graylog2.plugin;

import com.google.common.util.concurrent.Service;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import org.graylog2.plugin.alarms.callbacks.AlarmCallback;
import org.graylog2.plugin.filters.MessageFilter;
import org.graylog2.plugin.inject.Graylog2Module;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.graylog2.plugin.inputs.transports.Transport;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.plugin.rest.PluginRestResource;

import java.util.Collections;
import java.util.Set;

public abstract class PluginModule extends Graylog2Module {
    public Set<? extends PluginConfigBean> getConfigBeans() {
        return Collections.emptySet();
    }

    protected void addMessageInput(Class<? extends MessageInput> messageInputClass) {
        installInput(inputsMapBinder(), messageInputClass);
    }

    protected <T extends MessageInput> void addMessageInput(Class<T> messageInputClass,
                                                            Class<? extends MessageInput.Factory<T>> factoryClass) {
        installInput(inputsMapBinder(), messageInputClass, factoryClass);
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

        TypeLiteral<Class<? extends AlarmCallback>> type = new TypeLiteral<Class<? extends AlarmCallback>>() {
        };
        Multibinder<Class<? extends AlarmCallback>> alarmCallbackBinder = Multibinder.newSetBinder(binder(), type);
        alarmCallbackBinder.addBinding().toInstance(alarmCallbackClass);
    }

    protected void addInitializer(Class<? extends Service> initializerClass) {
        Multibinder<Service> serviceBinder = serviceBinder();
        serviceBinder.addBinding().to(initializerClass);
    }

    protected void addMessageOutput(Class<? extends MessageOutput> messageOutputClass) {
        installOutput(outputsMapBinder(), messageOutputClass);
    }

    protected <T extends MessageOutput> void addMessageOutput(Class<T> messageOutputClass,
                                                              Class<? extends MessageOutput.Factory<T>> factory) {
        installOutput(outputsMapBinder(), messageOutputClass, factory);
    }

    protected void addRestResource(Class<? extends PluginRestResource> restResourceClass) {
        MapBinder<String, PluginRestResource> pluginRestResourceMapBinder = MapBinder.newMapBinder(binder(), String.class, PluginRestResource.class).permitDuplicates();
        pluginRestResourceMapBinder.addBinding(this.getClass().getPackage().getName()).to(restResourceClass);
    }

    protected void addConfigBeans() {
        final Multibinder<PluginConfigBean> pluginConfigBeans = Multibinder.newSetBinder(binder(), PluginConfigBean.class);
        for (PluginConfigBean pluginConfigBean : getConfigBeans()) {
            pluginConfigBeans.addBinding().toInstance(pluginConfigBean);
        }
    }

    protected void addTransport(String name, Class<? extends Transport> transportClass) {
        installTransport(transportMapBinder(), name, transportClass);
    }

    protected void addTransport(String name,
                                Class<? extends Transport> transportClass,
                                Class<? extends Transport.Config> configClass,
                                Class<? extends Transport.Factory<? extends Transport>> factoryClass) {
        installTransport(transportMapBinder(), name, transportClass, configClass, factoryClass);
    }

    protected void addCodec(String name, Class<? extends Codec> codecClass) {
        installCodec(codecMapBinder(), name, codecClass);
    }

    protected void addCodec(String name,
                            Class<? extends Codec> codecClass,
                            Class<? extends Codec.Config> configClass,
                            Class<? extends Codec.Factory<? extends Codec>> factoryClass) {
        installCodec(codecMapBinder(), name, codecClass, configClass, factoryClass);
    }
}

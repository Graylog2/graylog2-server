/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.plugin;

import com.google.common.util.concurrent.Service;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import org.graylog2.plugin.alarms.callbacks.AlarmCallback;
import org.graylog2.plugin.dashboards.widgets.WidgetStrategy;
import org.graylog2.plugin.filters.MessageFilter;
import org.graylog2.plugin.indexer.retention.RetentionStrategy;
import org.graylog2.plugin.indexer.rotation.RotationStrategy;
import org.graylog2.plugin.messageprocessors.MessageProcessor;
import org.graylog2.plugin.inject.Graylog2Module;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.graylog2.plugin.inputs.transports.Transport;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.plugin.security.PasswordAlgorithm;
import org.graylog2.plugin.security.PluginPermissions;

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

    protected void addRotationStrategy(Class<? extends RotationStrategy> rotationStrategyClass) {
        installRotationStrategy(rotationStrategiesMapBinder(), rotationStrategyClass);
    }

    protected void addRetentionStrategy(Class<? extends RetentionStrategy> retentionStrategyClass) {
        installRetentionStrategy(retentionStrategyMapBinder(), retentionStrategyClass);
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
        MapBinder<String, Class<? extends PluginRestResource>> pluginRestResourceMapBinder =
                MapBinder.newMapBinder(binder(), new TypeLiteral<String>() {},
                                       new TypeLiteral<Class<? extends PluginRestResource>>() {})
                        .permitDuplicates();
        pluginRestResourceMapBinder.addBinding(this.getClass().getPackage().getName()).toInstance(restResourceClass);
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

    protected void addPasswordAlgorithm(String passwordAlgorithmName, Class<? extends PasswordAlgorithm> passwordAlgorithmClass) {
        passwordAlgorithmBinder().addBinding(passwordAlgorithmName).to(passwordAlgorithmClass);
    }

    protected Multibinder<MessageProcessor> processorBinder() {
        return Multibinder.newSetBinder(binder(), MessageProcessor.class);
    }

    protected Multibinder<MessageProcessor.Descriptor> processorDescriptorBinder() {
        return Multibinder.newSetBinder(binder(), MessageProcessor.Descriptor.class);
    }

    protected void addMessageProcessor(Class<? extends MessageProcessor> processorClass, Class<? extends MessageProcessor.Descriptor> descriptorClass) {
        processorBinder().addBinding().to(processorClass);
        processorDescriptorBinder().addBinding().to(descriptorClass);
    }

    protected <T extends WidgetStrategy> void addWidgetStrategy(Class<T> widgetStrategyClass, Class<? extends WidgetStrategy.Factory<T>> factory) {
        installWidgetStrategy(widgetStrategyBinder(), widgetStrategyClass, factory);
    }

    protected void addPermissions(Class<? extends PluginPermissions> permissionsClass) {
        installPermissions(permissionsBinder(), permissionsClass);
    }
}

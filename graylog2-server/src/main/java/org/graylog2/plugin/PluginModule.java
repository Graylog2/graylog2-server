/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.plugin;

import com.google.common.util.concurrent.Service;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import org.graylog.events.fields.providers.FieldValueProvider;
import org.graylog.events.notifications.EventNotification;
import org.graylog.events.notifications.EventNotificationConfig;
import org.graylog.events.processor.EventProcessor;
import org.graylog.events.processor.EventProcessorConfig;
import org.graylog.events.processor.EventProcessorParameters;
import org.graylog.events.processor.storage.EventStorageHandler;
import org.graylog.grn.GRNDescriptorProvider;
import org.graylog.grn.GRNType;
import org.graylog.scheduler.Job;
import org.graylog.scheduler.JobDefinitionConfig;
import org.graylog.scheduler.JobSchedule;
import org.graylog.scheduler.JobTriggerData;
import org.graylog.security.authservice.AuthServiceBackend;
import org.graylog.security.authservice.AuthServiceBackendConfig;
import org.graylog2.Configuration;
import org.graylog2.audit.AuditEventType;
import org.graylog2.audit.PluginAuditEventTypes;
import org.graylog2.audit.formatter.AuditEventFormatter;
import org.graylog2.contentpacks.constraints.ConstraintChecker;
import org.graylog2.contentpacks.facades.EntityFacade;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.migrations.Migration;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.alarms.callbacks.AlarmCallback;
import org.graylog2.plugin.filters.MessageFilter;
import org.graylog2.plugin.indexer.retention.RetentionStrategy;
import org.graylog2.plugin.indexer.rotation.RotationStrategy;
import org.graylog2.plugin.inject.Graylog2Module;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.graylog2.plugin.inputs.transports.Transport;
import org.graylog2.plugin.messageprocessors.MessageProcessor;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.plugin.security.PasswordAlgorithm;
import org.graylog2.plugin.security.PluginPermissions;
import org.graylog2.shared.messageq.MessageQueueAcknowledger;
import org.graylog2.shared.messageq.MessageQueueReader;
import org.graylog2.shared.messageq.MessageQueueWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ext.ExceptionMapper;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

public abstract class PluginModule extends Graylog2Module {
    private static final Logger log = LoggerFactory.getLogger(PluginModule.class);

    private Injector configInjector;

    // TODO: limit visibility?
    public void setConfigInjector(Injector configInjector) {
        this.configInjector = configInjector;
    }

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

    // This should only be used by plugins that have been built before Graylog 3.0.1.
    // See comments in MessageOutput.Factory and MessageOutput.Factory2 for details
    protected void addMessageOutput(Class<? extends MessageOutput> messageOutputClass) {
        installOutput(outputsMapBinder(), messageOutputClass);
    }

    // This should only be used by plugins that have been built before Graylog 3.0.1.
    // See comments in MessageOutput.Factory and MessageOutput.Factory2 for details
    protected <T extends MessageOutput> void addMessageOutput(Class<T> messageOutputClass,
                                                              Class<? extends MessageOutput.Factory<T>> factory) {
        installOutput(outputsMapBinder(), messageOutputClass, factory);
    }

    // This should be used by plugins that have been built for 3.0.1 or later.
    // See comments in MessageOutput.Factory and MessageOutput.Factory2 for details
    protected <T extends MessageOutput> void addMessageOutput2(Class<T> messageOutputClass,
                                                              Class<? extends MessageOutput.Factory2<T>> factory) {
        installOutput2(outputsMapBinder2(), messageOutputClass, factory);
    }

    protected void addRestResource(Class<? extends PluginRestResource> restResourceClass) {
        MapBinder<String, Class<? extends PluginRestResource>> pluginRestResourceMapBinder =
                MapBinder.newMapBinder(binder(), new TypeLiteral<String>() {},
                                       new TypeLiteral<Class<? extends PluginRestResource>>() {})
                        .permitDuplicates();
        pluginRestResourceMapBinder.addBinding(this.getClass().getPackage().getName()).toInstance(restResourceClass);
    }

    protected void addJerseyExceptionMapper(Class<? extends ExceptionMapper> exceptionMapperClass) {
        jerseyExceptionMapperBinder().addBinding().toInstance(exceptionMapperClass);
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

    protected void addPermissions(Class<? extends PluginPermissions> permissionsClass) {
        installPermissions(permissionsBinder(), permissionsClass);
    }

    protected void addAuditEventTypes(Class<? extends PluginAuditEventTypes> auditEventTypesClass) {
        installAuditEventTypes(auditEventTypesBinder(), auditEventTypesClass);
    }

    protected void addAuditEventFormatter(AuditEventType auditEventType, Class<? extends AuditEventFormatter> auditEventFormatterClass) {
        installAuditEventFormatter(auditEventFormatterMapBinder(), auditEventType, auditEventFormatterClass);
    }

    protected void addAlertCondition(String name,
                                     Class<? extends AlertCondition> alertConditionClass,
                                     Class<? extends AlertCondition.Factory> alertConditionFactoryClass) {
        installAlertConditionWithCustomName(alertConditionBinder(), name, alertConditionClass, alertConditionFactoryClass);
    }

    protected void addMigration(Class<? extends Migration> migrationClass) {
        migrationsBinder().addBinding().to(migrationClass);
    }

    protected void addEntityFacade(ModelType entityType, Class<? extends EntityFacade<?>> entityFacadeClass) {
        entityFacadeBinder().addBinding(entityType).to(entityFacadeClass);
    }

    protected void addConstraintChecker(Class<? extends ConstraintChecker> constraintCheckerClass) {
        constraintCheckerBinder().addBinding().to(constraintCheckerClass);
    }

    private MapBinder<String, EventProcessor.Factory> eventProcessorBinder() {
        return MapBinder.newMapBinder(binder(), String.class, EventProcessor.Factory.class);
    }

    protected void addEventProcessor(String name,
                                     Class<? extends EventProcessor> processorClass,
                                     Class<? extends EventProcessor.Factory> factoryClass,
                                     Class<? extends EventProcessorConfig> configClass,
                                     Class<? extends EventProcessorParameters> parametersClass) {
        install(new FactoryModuleBuilder().implement(EventProcessor.class, processorClass).build(factoryClass));
        eventProcessorBinder().addBinding(name).to(factoryClass);
        registerJacksonSubtype(configClass, name);
        registerJacksonSubtype(parametersClass, name);
    }

    private MapBinder<String, EventStorageHandler.Factory> eventStorageHandlerBinder() {
        return MapBinder.newMapBinder(binder(), String.class, EventStorageHandler.Factory.class);
    }

    protected void addEventStorageHandler(String name,
                                          Class<? extends EventStorageHandler> handlerClass,
                                          Class<? extends EventStorageHandler.Factory> factoryClass,
                                          Class<? extends EventStorageHandler.Config> configClass) {
        install(new FactoryModuleBuilder().implement(EventStorageHandler.class, handlerClass).build(factoryClass));
        eventStorageHandlerBinder().addBinding(name).to(factoryClass);
        registerJacksonSubtype(configClass, name);
    }

    private MapBinder<String, FieldValueProvider.Factory> eventFieldValueProviderBinder() {
        return MapBinder.newMapBinder(binder(), String.class, FieldValueProvider.Factory.class);
    }

    protected void addEventFieldValueProvider(String name,
                                              Class<? extends FieldValueProvider> fieldValueProviderClass,
                                              Class<? extends FieldValueProvider.Factory> factoryClass,
                                              Class<? extends FieldValueProvider.Config> configClass) {
        install(new FactoryModuleBuilder().implement(FieldValueProvider.class, fieldValueProviderClass).build(factoryClass));
        eventFieldValueProviderBinder().addBinding(name).to(factoryClass);
        registerJacksonSubtype(configClass, name);
    }

    private MapBinder<String, Job.Factory> jobBinder() {
        return MapBinder.newMapBinder(binder(), String.class, Job.Factory.class);
    }

    protected void addSchedulerJob(String name,
                                 Class<? extends Job> jobClass,
                                 Class<? extends Job.Factory> factoryClass,
                                 Class<? extends JobDefinitionConfig> configClass) {
        addSchedulerJob(name, jobClass, factoryClass, configClass, null);
    }

    protected void addSchedulerJob(String name,
                                   Class<? extends Job> jobClass,
                                   Class<? extends Job.Factory> factoryClass,
                                   Class<? extends JobDefinitionConfig> configClass,
                                   Class<? extends JobTriggerData> dataClass) {
        install(new FactoryModuleBuilder().implement(Job.class, jobClass).build(factoryClass));
        jobBinder().addBinding(name).to(factoryClass);
        registerJacksonSubtype(configClass, name);

        // Some jobs might not have a custom data class
        if (dataClass != null) {
            registerJacksonSubtype(dataClass, name);
        }
    }

    protected void addJobSchedulerSchedule(String name, Class<? extends JobSchedule> scheduleClass) {
        registerJacksonSubtype(scheduleClass, name);
    }

    private MapBinder<String, EventNotification.Factory> eventNotificationBinder() {
        return MapBinder.newMapBinder(binder(), String.class, EventNotification.Factory.class);
    }

    protected void addNotificationType(String name,
                                       Class<? extends EventNotificationConfig> notificationClass,
                                       Class<? extends EventNotification> handlerClass,
                                       Class<? extends EventNotification.Factory> factoryClass) {
        install(new FactoryModuleBuilder().implement(EventNotification.class, handlerClass).build(factoryClass));
        eventNotificationBinder().addBinding(name).to(factoryClass);
        registerJacksonSubtype(notificationClass, name);
    }

    protected void addGRNType(GRNType type, Class<? extends GRNDescriptorProvider> descriptorProvider) {
        final MapBinder<GRNType, GRNDescriptorProvider> mapBinder = MapBinder.newMapBinder(binder(), GRNType.class, GRNDescriptorProvider.class);
        mapBinder.addBinding(type).to(descriptorProvider);
    }

    protected MapBinder<String, AuthServiceBackend.Factory<? extends AuthServiceBackend>> authServiceBackendBinder() {
        return MapBinder.newMapBinder(
                binder(),
                TypeLiteral.get(String.class),
                new TypeLiteral<AuthServiceBackend.Factory<? extends AuthServiceBackend>>() {}
        );
    }

    protected void addAuthServiceBackend(String name,
            Class<? extends AuthServiceBackend> backendClass,
            Class<? extends AuthServiceBackend.Factory<? extends AuthServiceBackend>> factoryClass,
            Class<? extends AuthServiceBackendConfig> configClass) {
        install(new FactoryModuleBuilder().implement(AuthServiceBackend.class, backendClass).build(factoryClass));
        authServiceBackendBinder().addBinding(name).to(factoryClass);
        registerJacksonSubtype(configClass, name);
    }

    /**
     * @return A boolean indicating if the plugin is being loaded on Graylog Cloud. The graylog.cloud system property is
     * set in the startup sequence of the Graylog Cloud Plugin.
     */
    protected boolean isCloud() {
        return Boolean.parseBoolean(System.getProperty("graylog.cloud"));
    }

    /**
     * Install a message queue implementation. If any of the given classes implement the {@link Service} interface, they
     * will be registered as services.
     *
     * @param config            Configuration to check if the current journal mode requires binding of the
     *                          implementation
     * @param mode              Identifier for the journal mode value assigned to this implementation
     * @param readerClass       Reader implementation
     * @param writerClass       Writer implementation
     * @param acknowledgerClass Acknowledger implementation
     */
    // TODO: remove obsolete param
    protected void installMessageQueueImplementation(BaseConfiguration obsolete, String mode,
                                                     Class<? extends MessageQueueReader> readerClass,
                                                     Class<? extends MessageQueueWriter> writerClass,
                                                     Class<? extends MessageQueueAcknowledger> acknowledgerClass) {

        final Configuration config = configInjector.getInstance(Configuration.class);

        if (!config.getEffectiveMessageJournalMode().equals(mode)) {
            return;
        }

        log.info("Activating <{}> journal mode.", mode);

        bind(MessageQueueReader.class).to(readerClass).in(Scopes.SINGLETON);
        bind(MessageQueueWriter.class).to(writerClass).in(Scopes.SINGLETON);
        bind(MessageQueueAcknowledger.class).to(acknowledgerClass).in(Scopes.SINGLETON);

        //noinspection unchecked
        Stream.of(readerClass, writerClass, acknowledgerClass)
                .filter(Service.class::isAssignableFrom)
                .forEach(service ->
                        serviceBinder().addBinding().to((Class<? extends Service>) service).in(Scopes.SINGLETON));
    }
}

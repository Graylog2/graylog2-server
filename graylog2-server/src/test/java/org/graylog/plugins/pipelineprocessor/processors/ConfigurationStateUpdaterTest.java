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
package org.graylog.plugins.pipelineprocessor.processors;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;
import org.assertj.core.api.Assertions;
import org.bson.types.ObjectId;
import org.graylog.plugins.pipelineprocessor.codegen.PipelineClassloader;
import org.graylog.plugins.pipelineprocessor.db.RuleDao;
import org.graylog.plugins.pipelineprocessor.db.RuleService;
import org.graylog.plugins.pipelineprocessor.db.memory.InMemoryServicesModule;
import org.graylog.plugins.pipelineprocessor.functions.ProcessorFunctionsModule;
import org.graylog.plugins.pipelineprocessor.parser.FunctionRegistry;
import org.graylog2.database.NotFoundException;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.grok.GrokPatternService;
import org.graylog2.grok.InMemoryGrokPatternService;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.database.Persisted;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.database.users.User;
import org.graylog2.plugin.database.validators.ValidationResult;
import org.graylog2.plugin.database.validators.Validator;
import org.graylog2.plugin.streams.Output;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.rest.resources.streams.requests.CreateStreamRequest;
import org.graylog2.shared.SuppressForbidden;
import org.graylog2.shared.bindings.SchedulerBindings;
import org.graylog2.shared.bindings.providers.MetricRegistryProvider;
import org.graylog2.streams.StreamImpl;
import org.graylog2.streams.StreamService;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Ignore("code generation disabled")
public class ConfigurationStateUpdaterTest {
    private static final Logger log = LoggerFactory.getLogger(ConfigurationStateUpdaterTest.class);
    private PipelineInterpreter.State reload;

    @SuppressForbidden("Allow calling System#gc()")
    @Test
    public void testClassUnloading() {

        final Injector injector = Guice.createInjector(
                new InMemoryServicesModule(),
                new ProcessorFunctionsModule(),
                new SchedulerBindings(),
                binder -> binder.install(new FactoryModuleBuilder().build(PipelineInterpreter.State.Factory.class)),
                binder -> binder.bindConstant().annotatedWith(Names.named("generate_native_code")).to(true),
                binder -> binder.bindConstant().annotatedWith(Names.named("cached_stageiterators")).to(true),
                binder -> binder.bindConstant().annotatedWith(Names.named("processbuffer_processors")).to(1),
                binder -> binder.bind(StreamService.class).to(DummyStreamService.class),
                binder -> binder.bind(ClusterEventBus.class).toInstance(new ClusterEventBus("cluster-event-bus", Executors.newSingleThreadExecutor())),
                binder -> binder.bind(GrokPatternService.class).to(InMemoryGrokPatternService.class),
                binder -> binder.bind(FunctionRegistry.class).asEagerSingleton(),
                binder -> binder.bind(MetricRegistry.class).toProvider(MetricRegistryProvider.class).asEagerSingleton()
        );

        final MetricRegistry metricRegistry = injector.getInstance(MetricRegistry.class);
        final Gauge<Long> pipelineLoadedClasses = metricRegistry.register("jvm.cl.loaded-classes", (Gauge<Long>) () -> PipelineClassloader.loadedClasses.get());

        final RuleService ruleService = injector.getInstance(RuleService.class);
        ruleService.save(RuleDao.create("00001", "some rule", "awesome rule", "rule \"arrsome\" when true then let x = now(); end", null, null));
        final ConfigurationStateUpdater updater = injector.getInstance(ConfigurationStateUpdater.class);

        //noinspection unchecked
        final Gauge<Long> unloadedClasses = metricRegistry.getGauges((name, metric) -> name.startsWith("jvm.cl.unloaded")).get("jvm.cl.unloaded");
        long i = 0;
        final Long initialLoaded = pipelineLoadedClasses.getValue();
        while (i++ < 100) {
            final long initialUnloaded = unloadedClasses.getValue();
            this.reload = null;
            reload = updater.reload();

            if (i % 10 == 0) {
                System.gc();
                log.info("\nClassloading metrics:\n=====================");
                metricRegistry.getGauges((name, metric) -> name.startsWith("jvm.cl")).forEach((s, gauge) -> log.info("{} : {}", s, gauge.getValue()));
                Assertions.assertThat(unloadedClasses.getValue()).isGreaterThan(initialUnloaded);
            }
        }
        Assertions.assertThat(pipelineLoadedClasses.getValue()).isGreaterThan(initialLoaded);
    }


    private static class DummyStreamService implements StreamService {

        private final Map<String, Stream> store = new HashMap<>();

        @Override
        public Stream create(Map<String, Object> fields) {
            return new StreamImpl(fields);
        }

        @Override
        public Stream create(CreateStreamRequest cr, String userId) {
            Map<String, Object> streamData = Maps.newHashMap();
            streamData.put(StreamImpl.FIELD_TITLE, cr.title());
            streamData.put(StreamImpl.FIELD_DESCRIPTION, cr.description());
            streamData.put(StreamImpl.FIELD_CREATOR_USER_ID, userId);
            streamData.put(StreamImpl.FIELD_CREATED_AT, Tools.nowUTC());
            streamData.put(StreamImpl.FIELD_CONTENT_PACK, cr.contentPack());
            streamData.put(StreamImpl.FIELD_MATCHING_TYPE, cr.matchingType().toString());

            return create(streamData);
        }

        @Override
        public Stream load(String id) throws NotFoundException {
            final Stream stream = store.get(id);
            if (stream == null) {
                throw new NotFoundException();
            }
            return stream;
        }

        @Override
        public String save(Stream stream) throws ValidationException {
            return this.save((Persisted) stream);
        }

        @Override
        public String saveWithRulesAndOwnership(Stream stream, Collection<StreamRule> streamRules, User user) throws ValidationException {
            return save(stream);
        }

        @Override
        public void destroy(Stream stream) throws NotFoundException {
            if (store.remove(stream.getId()) == null) {
                throw new NotFoundException();
            }
        }

        @Override
        public List<Stream> loadAll() {
            return ImmutableList.copyOf(store.values());
        }

        @Override
        public Set<Stream> loadByIds(Collection<String> streamIds) {
            return streamIds.stream()
                    .map(store::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        }

        @Override
        public List<Stream> loadAllEnabled() {
            return store.values().stream().filter(stream -> !stream.getDisabled()).collect(Collectors.toList());
        }

        @Override
        public long count() {
            return store.size();
        }

        @Override
        public void pause(Stream stream) throws ValidationException {
            throw new IllegalStateException("no implemented");
        }

        @Override
        public void resume(Stream stream) throws ValidationException {
            throw new IllegalStateException("no implemented");
        }

        @Override
        public List<StreamRule> getStreamRules(Stream stream) throws NotFoundException {
            throw new IllegalStateException("no implemented");
        }

        @Override
        public List<Stream> loadAllWithConfiguredAlertConditions() {
            throw new IllegalStateException("no implemented");
        }

        @Override
        public List<AlertCondition> getAlertConditions(Stream stream) {
            throw new IllegalStateException("no implemented");
        }

        @Override
        public AlertCondition getAlertCondition(Stream stream,
                                                String conditionId) throws NotFoundException {
            throw new IllegalStateException("no implemented");
        }

        @Override
        public void addAlertCondition(Stream stream,
                                      AlertCondition condition) throws ValidationException {
            throw new IllegalStateException("no implemented");
        }

        @Override
        public void updateAlertCondition(Stream stream,
                                         AlertCondition condition) throws ValidationException {
            throw new IllegalStateException("no implemented");
        }

        @Override
        public void removeAlertCondition(Stream stream, String conditionId) {
            throw new IllegalStateException("no implemented");
        }

        @Override
        public void addAlertReceiver(Stream stream, String type, String name) {
            throw new IllegalStateException("no implemented");
        }

        @Override
        public void removeAlertReceiver(Stream stream, String type, String name) {
            throw new IllegalStateException("no implemented");
        }

        @Override
        public void addOutput(Stream stream, Output output) {
            throw new IllegalStateException("no implemented");
        }

        @Override
        public void addOutputs(ObjectId streamId, Collection<ObjectId> outputIds) {
            throw new IllegalStateException("no implemented");
        }

        @Override
        public void removeOutput(Stream stream, Output output) {
            throw new IllegalStateException("no implemented");
        }

        @Override
        public void removeOutputFromAllStreams(Output output) {
            throw new IllegalStateException("no implemented");
        }

        @Override
        public List<Stream> loadAllWithIndexSet(String indexSetId) {
            throw new IllegalStateException("no implemented");
        }

        @Override
        public <T extends Persisted> int destroy(T model) {
            throw new IllegalStateException("no implemented");
        }

        @Override
        public <T extends Persisted> int destroyAll(Class<T> modelClass) {
            throw new IllegalStateException("no implemented");
        }

        @Override
        public <T extends Persisted> String save(T model) throws ValidationException {
            store.put(model.getId(), (Stream) model);
            return model.getId();
        }

        @Override
        public <T extends Persisted> String saveWithoutValidation(T model) {
            throw new IllegalStateException("no implemented");
        }

        @Override
        public <T extends Persisted> Map<String, List<ValidationResult>> validate(T model,
                                                                                  Map<String, Object> fields) {
            throw new IllegalStateException("no implemented");
        }

        @Override
        public <T extends Persisted> Map<String, List<ValidationResult>> validate(T model) {
            throw new IllegalStateException("no implemented");
        }

        @Override
        public Map<String, List<ValidationResult>> validate(Map<String, Validator> validators,
                                                            Map<String, Object> fields) {
            throw new IllegalStateException("no implemented");
        }


        @Override
        public Set<String> indexSetIdsByIds(Collection<String> streamIds) {
            throw new IllegalStateException("not implemented");
        }
    }
}

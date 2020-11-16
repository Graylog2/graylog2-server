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
package org.graylog2.filters;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.graylog2.database.NotFoundException;
import org.graylog2.inputs.Input;
import org.graylog2.inputs.InputService;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.filters.MessageFilter;
import org.graylog2.rest.models.system.inputs.responses.InputCreated;
import org.graylog2.rest.models.system.inputs.responses.InputDeleted;
import org.graylog2.rest.models.system.inputs.responses.InputUpdated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class StaticFieldFilter implements MessageFilter {

    private static final Logger LOG = LoggerFactory.getLogger(StaticFieldFilter.class);

    private static final String NAME = "Static field appender";

    private final ConcurrentMap<String, List<Map.Entry<String, String>>> staticFields = new ConcurrentHashMap<>();

    private final InputService inputService;
    private final ScheduledExecutorService scheduler;

    @Inject
    public StaticFieldFilter(InputService inputService,
                             EventBus serverEventBus,
                             @Named("daemonScheduler") ScheduledExecutorService scheduler) {
        this.inputService = inputService;
        this.scheduler = scheduler;

        loadAllStaticFields();

        // TODO: This class needs lifecycle management to avoid leaking objects in the EventBus
        serverEventBus.register(this);
    }

    @Override
    public boolean filter(Message msg) {
        if (msg.getSourceInputId() == null)
            return false;

        for(final Map.Entry<String, String> field : staticFields.getOrDefault(msg.getSourceInputId(), Collections.emptyList())) {
            if(!msg.hasField(field.getKey())) {
                msg.addField(field.getKey(), field.getValue());
            } else {
                LOG.debug("Message already contains field [{}]. Not overwriting.", field.getKey());
            }
        }

        return false;
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void handleInputCreate(final InputCreated event) {
        LOG.debug("Load static fields for input <{}>", event.id());
        scheduler.submit(() -> loadStaticFields(event.id()));
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void handleInputDelete(final InputDeleted event) {
        LOG.debug("Removing input from static fields cache <{}>", event.id());
        staticFields.remove(event.id());
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void handleInputUpdate(final InputUpdated event) {
        scheduler.submit(() -> loadStaticFields(event.id()));
    }

    private void loadAllStaticFields() {
        try {
            inputService.all().forEach(input -> loadStaticFields(input.getId()));
        } catch (Exception e) {
            LOG.error("Unable to load static fields for all inputs", e);
        }
    }

    private void loadStaticFields(final String inputId) {
        LOG.debug("Re-loading static fields for input <{}> into cache.", inputId);
        try {
            final Input input = inputService.find(inputId);
            staticFields.put(inputId, ImmutableList.copyOf(inputService.getStaticFields(input)));
        } catch (NotFoundException e) {
            LOG.warn("Unable to load input: {}", e.getMessage());
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getPriority() {
        // runs second of the built-in filters
        return 20;
    }

}

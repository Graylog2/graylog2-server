/*
 * Copyright 2013-2014 TORCH GmbH
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

package org.graylog2.inputs;

import com.google.common.collect.Lists;
import org.graylog2.Core;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationImpl;
import org.graylog2.notifications.NotificationService;
import org.graylog2.notifications.NotificationServiceImpl;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.inputs.Extractor;
import org.graylog2.plugin.inputs.InputState;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.shared.inputs.InputRegistry;
import org.graylog2.shared.inputs.NoSuchInputTypeException;
import org.graylog2.system.activities.Activity;

import java.util.List;
import java.util.Map;

public class ServerInputRegistry extends InputRegistry {
    protected final Core _core;
    protected final InputService inputService;
    protected final NotificationService notificationService;
    public ServerInputRegistry(Core core) {
        super(core);
        this._core = core;
        this.inputService = new InputServiceImpl(core.getMongoConnection());
        this.notificationService = new NotificationServiceImpl(core.getMongoConnection());
    }

    public static MessageInput getMessageInput(Input io, Core core) throws NoSuchInputTypeException, ConfigurationException {
        MessageInput input = InputRegistry.factory(io.getType());

        // Add all standard fields.
        input.initialize(new Configuration(io.getConfiguration()), core);
        input.setTitle(io.getTitle());
        input.setCreatorUserId(io.getCreatorUserId());
        input.setPersistId(io.getId());
        input.setCreatedAt(io.getCreatedAt());
        if (io.isGlobal())
            input.setGlobal(true);

        // Add extractors.
        for (Extractor extractor : io.getExtractors()) {
            input.addExtractor(extractor.getId(), extractor);
        }

        // Add static fields.
        for (Map.Entry<String, String> field : io.getStaticFields().entrySet()) {
            input.addStaticField(field.getKey(), field.getValue());
        }

        input.checkConfiguration();

        return input;
    }

    protected List<MessageInput> getAllPersisted() {
        List<MessageInput> result = Lists.newArrayList();

        for (Input io : inputService.allOfThisNode(_core)) {
            MessageInput input = null;
            try {
                input = getMessageInput(io, _core);
                result.add(input);
            } catch (NoSuchInputTypeException e) {
                LOG.warn("Cannot launch persisted input. No such type [{}].", io.getType());
            } catch (ConfigurationException e) {
                LOG.error("Missing or invalid input plugin configuration.", e);
            }
        }

        return result;
    }

    public void cleanInput(MessageInput messageInput) {
        Input input = inputService.find(messageInput.getPersistId());
        inputService.destroy(input);
    }

    @Override
    protected void finishedLaunch(InputState state) {
        switch (state.getState()) {
            case RUNNING:
                notificationService.fixed(Notification.Type.NO_INPUT_RUNNING);
                String msg = "Completed starting [" + state.getMessageInput().getClass().getCanonicalName() + "] input with ID <" + state.getMessageInput().getId() + ">";
                _core.getActivityWriter().write(new Activity(msg, InputRegistry.class));
                break;
            case FAILED:
                _core.getActivityWriter().write(new Activity(state.getDetailedMessage(), InputRegistry.class));
                Notification notification = notificationService.buildNow();
                notification.addType(NotificationImpl.Type.INPUT_FAILED_TO_START).addSeverity(NotificationImpl.Severity.NORMAL);
                notification.addThisNode(_core);
                notification.addDetail("input_id", state.getMessageInput().getId());
                notification.addDetail("reason", state.getDetailedMessage());
                notificationService.publishIfFirst(notification);
                break;
        }
    }

    @Override
    protected void finishedTermination(InputState state) {
    }

    @Override
    protected void finishedStop(InputState inputState) {
    }
}
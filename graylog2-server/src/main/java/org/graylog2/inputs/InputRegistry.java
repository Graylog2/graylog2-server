/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
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
 *
 */
package org.graylog2.inputs;


import com.beust.jcommander.internal.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mongodb.BasicDBObject;
import org.bson.types.ObjectId;
import org.graylog2.Core;
import org.graylog2.notifications.Notification;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.inputs.Extractor;
import org.graylog2.plugin.inputs.InputState;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.MisfireException;
import org.graylog2.system.activities.Activity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class InputRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(InputRegistry.class);

    private final Core core;
    private final List<InputState> inputStates;
    private final Map<String, String> availableInputs;

    private ExecutorService executor = Executors.newCachedThreadPool(
            new ThreadFactoryBuilder().setNameFormat("inputs-%d").build()
    );
    private static Map<String, ClassLoader> classLoaders = Maps.newHashMap();

    public InputRegistry(Core core) {
        this.core = core;
        inputStates = Lists.newArrayList();
        availableInputs = Maps.newHashMap();
    }

    public String launch(final MessageInput input, String id) {
        final InputState inputState = new InputState(input, id);
        inputStates.add(inputState);

        executor.submit(new Runnable() {
            @Override
            public void run() {
                LOG.info("Starting [{}] input with ID <{}>", input.getClass().getCanonicalName(), input.getId());
                try {
                    inputState.setState(InputState.InputStateType.STARTING);
                    input.launch();
                    Notification.fixed(core, Notification.Type.NO_INPUT_RUNNING);
                    inputState.setState(InputState.InputStateType.RUNNING);
                    String msg = "Completed starting [" + input.getClass().getCanonicalName() + "] input with ID <" + input.getId() + ">";
                    core.getActivityWriter().write(new Activity(msg, InputRegistry.class));
                    LOG.info(msg);
                } catch (MisfireException e) {
                    handleLaunchException(e, input, inputState);
                } catch (Exception e) {
                    handleLaunchException(e, input, inputState);
                }
            }
        });

        return inputState.getId();
    }

    protected void handleLaunchException(Throwable e, MessageInput input, InputState inputState) {
        StringBuilder msg = new StringBuilder("The [" + input.getClass().getCanonicalName() + "] input with ID <" + input.getId() + "> misfired. Reason: ");

        StringBuilder causeMsg = new StringBuilder(e.getMessage());

        // Go down the whole cause chain to build a message that provides as much information as possible.
        int maxLevel = 7; // ;)
        Throwable cause = e.getCause();
        for (int i = 0; i < maxLevel; i++) {
            if (cause == null) {
                break;
            }

            causeMsg.append(", ").append(cause.getMessage());
            cause = cause.getCause();
        }

        msg.append(causeMsg);

        core.getActivityWriter().write(new Activity(msg.toString(), InputRegistry.class));
        LOG.error(msg.toString(), e);

        Notification notification = Notification.buildNow(core);
        notification.addType(Notification.Type.INPUT_FAILED_TO_START).addSeverity(Notification.Severity.NORMAL);
        notification.addThisNode();
        notification.addDetail("input_id", input.getId());
        notification.addDetail("reason", causeMsg.toString());
        notification.publishIfFirst();

        // Clean up.
        //cleanInput(input);

        inputState.setState(InputState.InputStateType.FAILED);
    }

    public String launch(final MessageInput input) {
        return launch(input, UUID.randomUUID().toString());
    }

    public List<InputState> getInputStates() {
        return inputStates;
    }

    public List<InputState> getRunningInputs() {
        List<InputState> runningInputs = Lists.newArrayList();
        for (InputState inputState : inputStates) {
            if (inputState.getState() == InputState.InputStateType.RUNNING)
                runningInputs.add(inputState);
        }
        return inputStates;
    }

    public boolean hasTypeRunning(Class klazz) {
        for (InputState inputState : inputStates) {
            if (inputState.getMessageInput().getClass().equals(klazz)) {
                return true;
            }
        }

        return false;
    }

    public Map<String, String> getAvailableInputs() {
        return availableInputs;
    }

    public int runningCount() {
        return getRunningInputs().size();
    }

    public static MessageInput factory(String type) throws NoSuchInputTypeException {
        try {
            final ClassLoader classLoader = lookupClassLoader(type);
            if (classLoader == null) {
                throw new NoSuchInputTypeException("There is no classloader to load input of type <" + type + ">.");
            }
            Class c = Class.forName(type, true, classLoader);
            return (MessageInput) c.newInstance();
        } catch (ClassNotFoundException e) {
             throw new NoSuchInputTypeException("There is no input of type <" + type + "> registered.");
        } catch (Exception e) {
            throw new RuntimeException("Could not create input of type <" + type + ">", e);
        }
    }

    private static ClassLoader lookupClassLoader(String type) {
        return classLoaders.get(type);
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

    public void register(Class clazz, String name) {
        classLoaders.put(clazz.getCanonicalName(), clazz.getClassLoader());
        availableInputs.put(clazz.getCanonicalName(), name);
    }

    public void cleanInput(MessageInput input) {
        // Remove in Mongo.
        Input.destroy(new BasicDBObject("_id", new ObjectId(input.getPersistId())), core, Input.COLLECTION);
    }

    public void removeFromRunning(MessageInput input) {
        // Remove from running list.
        InputState thisInputState = null;
        for (InputState inputState : inputStates) {
            if (inputState.getMessageInput().equals(input)) {
                thisInputState = inputState;
            }
        }
        inputStates.remove(thisInputState);
    }

    public void launchPersisted() {
        for (Input io : Input.allOfThisNode(core)) {
            MessageInput input = null;
            try {
                input = InputRegistry.getMessageInput(io, core);
            } catch (NoSuchInputTypeException e) {
                LOG.error("Cannot launch persisted input. No such type [{}].", io.getType());
                continue;
            } catch (ConfigurationException e) {
                LOG.error("Missing or invalid input plugin configuration.", e);
                continue;
            }

            launch(input, io.getInputId());
        }
    }


    public MessageInput getRunningInput(String inputId) {
        for (InputState inputState : inputStates) {
            if (inputState.getMessageInput().getId().equals(inputId))
                return inputState.getMessageInput();
        }

        return null;
    }

    public InputState getRunningInputState(String inputStateId) {
        for (InputState inputState : inputStates) {
            if (inputState.getId().equals(inputStateId))
                return inputState;
        }

        return null;
    }
}

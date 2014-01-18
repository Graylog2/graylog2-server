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
package org.graylog2.shared.inputs;


import com.beust.jcommander.internal.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.graylog2.plugin.InputHost;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.inputs.InputState;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.MisfireException;
import org.graylog2.shared.inputs.NoSuchInputTypeException;
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
public abstract class InputRegistry {

    protected static final Logger LOG = LoggerFactory.getLogger(InputRegistry.class);
    protected static final Map<String, ClassLoader> classLoaders = Maps.newHashMap();
    protected final InputHost core;
    protected final List<InputState> inputStates = Lists.newArrayList();
    protected final Map<String, String> availableInputs = Maps.newHashMap();
    protected final ExecutorService executor = Executors.newCachedThreadPool(
            new ThreadFactoryBuilder().setNameFormat("inputs-%d").build()
    );

    public InputRegistry(InputHost core) {
        this.core = core;
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

    protected static ClassLoader lookupClassLoader(String type) {
        return classLoaders.get(type);
    }

    public String launch(final MessageInput input, String id) {
        return launch(input, id, false);
    }

    protected abstract void finishedLaunch(InputState state);

    protected abstract void finishedTermination(InputState state);

    public String launch(final MessageInput input, String id, boolean register) {
        final InputState inputState = new InputState(input, id);
        inputStates.add(inputState);

        executor.submit(new Runnable() {
            @Override
            public void run() {
                LOG.info("Starting [{}] input with ID <{}>", input.getClass().getCanonicalName(), input.getId());
                try {
                    inputState.setState(InputState.InputStateType.STARTING);
                    input.launch();
                    inputState.setState(InputState.InputStateType.RUNNING);
                    String msg = "Completed starting [" + input.getClass().getCanonicalName() + "] input with ID <" + input.getId() + ">";
                    LOG.info(msg);
                } catch (MisfireException e) {
                    handleLaunchException(e, input, inputState);
                } catch (Exception e) {
                    handleLaunchException(e, input, inputState);
                } finally {
                    finishedLaunch(inputState);
                }
            }
        });

        return inputState.getId();
    }

    protected void handleLaunchException(Throwable e, MessageInput input, InputState inputState) {
        StringBuilder msg = new StringBuilder("The [" + input.getClass().getCanonicalName() + "] input with ID <" + input.getId() + "> misfired. Reason: ");

        String causeMsg = extractMessageCause(e);

        msg.append(causeMsg);

        LOG.error(msg.toString(), e);

        // Clean up.
        //cleanInput(input);

        inputState.setState(InputState.InputStateType.FAILED);
        inputState.setDetailedMessage(causeMsg);
    }

    private String extractMessageCause(Throwable e) {
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
        return causeMsg.toString();
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

    public void register(Class clazz, String name) {
        classLoaders.put(clazz.getCanonicalName(), clazz.getClassLoader());
        availableInputs.put(clazz.getCanonicalName(), name);
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

    public String launchPersisted(MessageInput input) {
        try {
            input.checkConfiguration();
        } catch (ConfigurationException e) {
            LOG.error("Missing or invalid input input configuration.", e);
            return null;
        }

        return launch(input);
    }

    protected abstract List<MessageInput> getAllPersisted();

    public void launchAllPersisted() {
        for (MessageInput input : getAllPersisted()) {
            launchPersisted(input);
        }
    }

    public InputState terminate(MessageInput input) {
        InputState inputState = getRunningInputState(input.getId());

        if (inputState == null)
            return null;

        input.stop();
        removeFromRunning(input);
        inputState.setState(InputState.InputStateType.TERMINATED);
        finishedTermination(inputState);

        return inputState;
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
            if (inputState.getMessageInput().getId().equals(inputStateId))
                return inputState;
        }

        return null;
    }

    public abstract void cleanInput(MessageInput input);

    public MessageInput getPersisted(String inputId) {
        for (MessageInput input : getAllPersisted()) {
            if (input.getId().equals(inputId))
                return input;
        }

        return null;
    }
}

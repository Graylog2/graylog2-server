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
package org.graylog2.radio.inputs;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.graylog2.plugin.GraylogServer;
import org.graylog2.plugin.InputHost;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.MisfireException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class InputRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(InputRegistry.class);

    private final InputHost server;
    private Map<String, MessageInput> runningInputs;
    private Map<String, String> availableInputs;

    private ExecutorService executor = Executors.newCachedThreadPool(
            new ThreadFactoryBuilder().setNameFormat("inputs-%d").build()
    );

    public InputRegistry(InputHost server) {
        this.server = server;
        runningInputs = Maps.newHashMap();
        availableInputs = Maps.newHashMap();
    }

    public String launch(final MessageInput input, String id) {
        input.setId(id);
        runningInputs.put(id, input);

        executor.submit(new Runnable() {
            @Override
            public void run() {
                LOG.info("Starting [{}] input with ID <{}>", input.getClass().getCanonicalName(), input.getId());
                try {
                    input.launch();
                } catch (MisfireException e) {
                    StringBuilder msg = new StringBuilder("The [" + input.getClass().getCanonicalName() + "] input with ID <" + input.getId() + "> " +
                            "was accepted but misfired. Reason: ").append(e.getMessage());

                    // Go down the whole cause chain to build a message that provides as much information as possible.
                    int maxLevel = 7; // ;)
                    Throwable cause = e.getCause();
                    for (int i = 0; i < maxLevel; i++) {
                        if (cause == null) {
                            break;
                        }

                        msg.append(", ").append(cause.getMessage());
                        cause = cause.getCause();
                    }

                    LOG.error(msg.toString(), e);

                    // Clean up.
                    cleanInput(input);
                } catch(Exception e) {
                    LOG.error("Error in input <{}>", input.getId(), e);
                }
            }
        });

        return id;
    }


    public void cleanInput(MessageInput input) {
        // Remove from running list.
        getRunningInputs().remove(input.getId());
    }

    public static MessageInput factory(String type) throws NoSuchInputTypeException {
        try {
            Class c = Class.forName(type);
            return (MessageInput) c.newInstance();
        } catch (ClassNotFoundException e) {
            throw new NoSuchInputTypeException("There is no input of type <" + type + "> registered.");
        } catch (Exception e) {
            throw new RuntimeException("Could not create input of type <" + type + ">", e);
        }
    }

    public boolean hasTypeRunning(Class klazz) {
        for (MessageInput input : runningInputs.values()) {
            if (input.getClass().equals(klazz)) {
                return true;
            }
        }

        return false;
    }

    public Map<String, MessageInput> getRunningInputs() {
        return runningInputs;
    }

}

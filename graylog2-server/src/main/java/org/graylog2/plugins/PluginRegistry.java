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

package org.graylog2.plugins;

import org.graylog2.Configuration;
import org.graylog2.outputs.OutputRegistry;
import org.graylog2.plugin.Plugin;
import org.graylog2.plugin.alarms.callbacks.AlarmCallback;
import org.graylog2.plugin.alarms.transports.Transport;
import org.graylog2.plugin.filters.MessageFilter;
import org.graylog2.plugin.initializers.Initializer;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.shared.filters.FilterRegistry;
import org.graylog2.shared.inputs.InputRegistry;
import org.graylog2.shared.plugins.PluginLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class PluginRegistry {
    private final Logger LOG = LoggerFactory.getLogger(PluginRegistry.class);
    private final Configuration configuration;
    private final FilterRegistry filterRegistry;
    private final InputRegistry inputRegistry;
    private final OutputRegistry outputRegistry;

    @Inject
    public PluginRegistry(Configuration configuration,
                          FilterRegistry filterRegistry,
                          InputRegistry inputRegistry,
                          OutputRegistry outputRegistry) {
        this.configuration = configuration;
        this.filterRegistry = filterRegistry;
        this.inputRegistry = inputRegistry;
        this.outputRegistry = outputRegistry;
    }

    public <A> void register(Class<A> type, String subDirectory) {
        LegacyPluginLoader<A> pl = new LegacyPluginLoader<A>(configuration.getPluginDir(), subDirectory, type);
        for (A plugin : pl.getPlugins()) {
            LOG.info("Loaded <{}> plugin [{}].", type.getSimpleName(), plugin.getClass().getCanonicalName());

            if (plugin instanceof MessageFilter) {
                filterRegistry.register((MessageFilter) plugin);
            } else if (plugin instanceof MessageInput) {
                inputRegistry.register(plugin.getClass(), ((MessageInput) plugin).getName());
            } else if (plugin instanceof MessageOutput) {
                outputRegistry.register((MessageOutput) plugin);
            } else if (plugin instanceof AlarmCallback) {
                //registerAlarmCallback((AlarmCallback) plugin);
                throw new RuntimeException("Not able to handle AlarmCallback plugins yet!");
            } else if (plugin instanceof Initializer) {
                //initializers.register((Initializer) plugin);
                throw new RuntimeException("Not able to handle Initializer plugins yet!");
            } else if (plugin instanceof Transport) {
                //registerTransport((Transport) plugin);
                throw new RuntimeException("Not able to handle Transport plugins yet!");
            } else {
                LOG.error("Could not load plugin [{}] - Not supported type.", plugin.getClass().getCanonicalName());
            }
        }

        PluginLoader pluginLoader = new PluginLoader(new File(configuration.getPluginDir()));
        for (Plugin plugin : pluginLoader.loadPlugins()) {
            for (Class<? extends MessageInput> inputClass : plugin.inputs()) {
                final MessageInput messageInput;
                try {
                    messageInput = inputClass.newInstance();
                    inputRegistry.register(inputClass, messageInput.getName());
                } catch (Exception e) {
                    LOG.error("Unable to register message input " + inputClass.getCanonicalName(), e);
                }
            }
        }
    }
}

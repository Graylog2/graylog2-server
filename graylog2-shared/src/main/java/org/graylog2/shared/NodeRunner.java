/**
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
package org.graylog2.shared;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.inject.Graylog2Module;
import org.graylog2.shared.bindings.GenericBindings;
import org.graylog2.shared.bindings.InstantiationService;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class NodeRunner {
    private static final Logger LOG = LoggerFactory.getLogger(NodeRunner.class);

    protected static List<Module> getBindingsModules(InstantiationService instantiationService, Module... specificModules) {
        List<Module> result = Lists.newArrayList();
        result.add(new GenericBindings(instantiationService));
        Reflections reflections = new Reflections("org.graylog2.shared.bindings");
        final Set<Class<? extends AbstractModule>> generic = reflections.getSubTypesOf(AbstractModule.class);
        final Set<Class<? extends Graylog2Module>> gl2Modules = reflections.getSubTypesOf(Graylog2Module.class);
        for (Class<? extends Module> type : Iterables.concat(generic, gl2Modules)) {
            // skip the GenericBindings module, because we have already instantiated it above, avoids a bogus log message
            if (type.equals(GenericBindings.class)) {
                continue;
            }
            try {
                Constructor<? extends Module> constructor = type.getConstructor();
                Module module = constructor.newInstance();
                result.add(module);
            } catch (InstantiationException | InvocationTargetException | IllegalAccessException e) {
                LOG.error("Unable to instantiate Module {}: {}", type, e);
            } catch (NoSuchMethodException e) {
                LOG.info("No constructor found for guice module {}", type);
            }
        }

        result.addAll(Arrays.asList(specificModules));
        return result;
    }

    protected static void savePidFile(final String pidFile) {
        final String pid = Tools.getPID();
        final Path pidFilePath = Paths.get(pidFile);
        pidFilePath.toFile().deleteOnExit();

        try {
            if (pid == null || pid.isEmpty() || pid.equals("unknown")) {
                throw new Exception("Could not determine PID.");
            }

            Files.write(pidFilePath, pid.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            LOG.error("Could not write PID file: " + e.getMessage(), e);
            System.exit(1);
        }
    }
}

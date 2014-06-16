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

package org.graylog2.shared;

import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import org.apache.commons.io.IOUtils;
import org.glassfish.hk2.extension.ServiceLocatorGenerator;
import org.glassfish.jersey.internal.inject.Injections;
import org.graylog2.plugin.Tools;
import org.graylog2.shared.bindings.GenericBindings;
import org.graylog2.shared.bindings.InstantiationService;
import org.graylog2.shared.bindings.OwnServiceLocatorGenerator;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class NodeRunner {
    private static final Logger LOG = LoggerFactory.getLogger(NodeRunner.class);

    protected static List<Module> getBindingsModules(InstantiationService instantiationService, Module... specificModules) {
        List<Module> result = Lists.newArrayList();
        result.add(new GenericBindings(instantiationService));
        Reflections reflections = new Reflections("org.graylog2.shared.bindings");
        for (Class<? extends Module> type : reflections.getSubTypesOf(AbstractModule.class)) {
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

    protected static void monkeyPatchHK2(Injector injector) {
        ServiceLocatorGenerator ownGenerator = new OwnServiceLocatorGenerator(injector);
        try {
            Field field = Injections.class.getDeclaredField("generator");
            field.setAccessible(true);
            Field modifiers = Field.class.getDeclaredField("modifiers");
            modifiers.setAccessible(true);
            modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);

            field.set(null, ownGenerator);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            LOG.error("Monkey patching Jersey's HK2 failed: ", e);
            System.exit(-1);
        }

        /*ServiceLocatorFactory factory = ServiceLocatorFactory.getInstance();
        factory.addListener(new HK2ServiceLocatorListener(injector));*/

    }

    protected static void savePidFile(String pidFile) {

        String pid = Tools.getPID();
        Writer pidFileWriter = null;

        try {
            if (pid == null || pid.isEmpty() || pid.equals("unknown")) {
                throw new Exception("Could not determine PID.");
            }

            pidFileWriter = new FileWriter(pidFile);
            IOUtils.write(pid, pidFileWriter);
        } catch (Exception e) {
            LOG.error("Could not write PID file: " + e.getMessage(), e);
            System.exit(1);
        } finally {
            IOUtils.closeQuietly(pidFileWriter);
            // make sure to remove our pid when we exit
            new File(pidFile).deleteOnExit();
        }
    }
}

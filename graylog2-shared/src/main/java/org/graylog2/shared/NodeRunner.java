/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.graylog2.shared;

import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import org.graylog2.plugin.Tools;
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

public class NodeRunner {
    private static final Logger LOG = LoggerFactory.getLogger(NodeRunner.class);

    protected static List<Module> getBindingsModules(InstantiationService instantiationService, Module... specificModules) {
        List<Module> result = Lists.newArrayList();
        result.add(new GenericBindings(instantiationService));
        Reflections reflections = new Reflections("org.graylog2.shared.bindings");
        for (Class<? extends Module> type : reflections.getSubTypesOf(AbstractModule.class)) {
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

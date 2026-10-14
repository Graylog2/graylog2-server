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
package org.graylog.api;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.Resource;
import io.github.classgraph.ScanResult;
import org.graylog2.plugin.PluginModule;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Determines the URL prefix under which the server serves plugin REST resources: the package of the
 * {@link PluginModule} that registers them via {@code addRestResource}, as {@link PluginModule} does at runtime.
 * Resources registered via {@code addSystemRestResource} are not prefixed, even if their module is a {@link PluginModule}.
 */
public final class PluginRestResourcePrefixes {
    private static final String ADD_REST_RESOURCE = "addRestResource";

    private PluginRestResourcePrefixes() {}

    public static Map<Class<?>, String> forResources(Set<Class<?>> resourceClasses, String... packageNames) {
        final Map<String, Set<String>> registeringPackages = new HashMap<>();
        try (final ScanResult scanResult = new ClassGraph().enableClassInfo().acceptPackages(packageNames).scan()) {
            for (final ClassInfo module : scanResult.getSubclasses(PluginModule.class.getName())) {
                if (module.isAbstract()) {
                    continue;
                }
                registeredResources(module).forEach(resource -> registeringPackages
                        .computeIfAbsent(resource, name -> new TreeSet<>())
                        .add(module.getPackageName()));
            }
        }

        final Map<Class<?>, String> prefixes = new HashMap<>();
        for (final Class<?> resourceClass : resourceClasses) {
            final Set<String> packages = registeringPackages.getOrDefault(resourceClass.getName(), Set.of());
            if (packages.size() > 1) {
                throw new IllegalStateException(f("%s is registered by plugin modules in %s", resourceClass.getName(), packages));
            }
            packages.stream().findFirst().ifPresent(prefix -> prefixes.put(resourceClass, prefix));
        }
        return prefixes;
    }

    // Collects the class literals passed directly to addRestResource(...) in the module's bytecode.
    private static Set<String> registeredResources(ClassInfo module) {
        final Set<String> resources = new HashSet<>();
        try (final Resource classFile = module.getResource()) {
            new ClassReader(classFile.load()).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                    return new MethodVisitor(Opcodes.ASM9) {
                        private String lastClassLiteral;

                        @Override
                        public void visitLdcInsn(Object value) {
                            lastClassLiteral = value instanceof Type type ? type.getClassName() : null;
                        }

                        @Override
                        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                            if (ADD_REST_RESOURCE.equals(name) && lastClassLiteral != null) {
                                resources.add(lastClassLiteral);
                            }
                            lastClassLiteral = null;
                        }
                    };
                }
            }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return resources;
    }
}

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
package org.graylog.plugins.pipelineprocessor.compiler;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import org.graylog.plugins.pipelineprocessor.ast.functions.Function;
import org.graylog.plugins.pipelineprocessor.ast.functions.PipelineFastPath;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

/** Every function with a @PipelineFastPath method must match its descriptor; the expected set guards against silent loss. */
class FastPathSignatureTest {
    static final Set<String> EXPECTED_FAST_PATH_FUNCTIONS = Set.of();

    @Test
    void allFastPathSignaturesMatchTheirDescriptors() throws Exception {
        final Set<String> found = new TreeSet<>();
        try (ScanResult scan = new ClassGraph().enableClassInfo().enableMethodInfo().enableAnnotationInfo()
                .acceptPackages("org.graylog.plugins.pipelineprocessor.functions").scan()) {
            // subclasses inherit annotated methods but are not reported by getClassesWithMethodAnnotation
            final Set<ClassInfo> candidates = new LinkedHashSet<>();
            for (ClassInfo info : scan.getClassesWithMethodAnnotation(PipelineFastPath.class.getName())) {
                candidates.add(info);
                candidates.addAll(info.getSubclasses());
            }
            for (ClassInfo info : candidates) {
                if (info.isAbstract()) {
                    continue;
                }
                final Function<?> function = (Function<?>) info.loadClass().getDeclaredConstructor().newInstance();
                assertThat(FastPathResolver.validate(function)).as(info.getName()).isNotNull();
                found.add(function.descriptor().name());
            }
        }
        assertThat(found).containsAll(EXPECTED_FAST_PATH_FUNCTIONS);
    }
}

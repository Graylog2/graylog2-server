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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/** Defines generated rule classes as hidden classes, which are unloaded once their CompiledRule is unreachable. */
final class CompiledRuleLoader {
    // the generated classes live in this package and resolve server types through this class's loader
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static final MethodType NO_ARGS = MethodType.methodType(void.class);

    private CompiledRuleLoader() {
    }

    static CompiledRule load(RuleClassEmitter.Emitted emitted) throws Throwable {
        final MethodHandles.Lookup hidden = LOOKUP.defineHiddenClassWithClassData(emitted.bytes(), emitted.classData(), true);
        return (CompiledRule) hidden.findConstructor(hidden.lookupClass(), NO_ARGS).invoke();
    }
}

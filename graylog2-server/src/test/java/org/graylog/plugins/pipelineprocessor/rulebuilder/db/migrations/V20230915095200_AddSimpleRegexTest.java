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
package org.graylog.plugins.pipelineprocessor.rulebuilder.db.migrations;

import org.graylog.plugins.pipelineprocessor.ast.functions.Function;
import org.graylog.plugins.pipelineprocessor.functions.strings.RegexMatch;
import org.graylog.plugins.pipelineprocessor.parser.FunctionRegistry;
import org.graylog.plugins.pipelineprocessor.rulebuilder.db.RuleFragment;
import org.graylog.plugins.pipelineprocessor.rulebuilder.parser.BaseFragmentTest;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Map;

public class V20230915095200_AddSimpleRegexTest extends BaseFragmentTest {

    @BeforeClass
    public static void initialize() {
        final Map<String, Function<?>> functions = commonFunctions();
        functions.put(RegexMatch.NAME, new RegexMatch());
        functionRegistry = new FunctionRegistry(functions);
    }

    @Test
    public void testSimpleRegex() {
        RuleFragment fragment = V20230915095200_AddSimpleRegex.createSimpleRegex();
        Map<String, Object> params = Map.of("pattern", "^([a-zA-z]+)\\\\s(\\\\d+)$", "value", "Answer 42");
        createFragmentSource(fragment, params);
    }

}

/*
 *  Copyright (C) 2020 Graylog, Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the Server Side Public License, version 1,
 *  as published by MongoDB, Inc.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  Server Side Public License for more details.
 *
 *  You should have received a copy of the Server Side Public License
 *  along with this program. If not, see
 *  <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.plugins.pipelineprocessor.rulebuilder.db;

import com.google.common.collect.ImmutableList;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.parser.FunctionRegistry;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import static org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor.string;

public class RuleFragmentService {

    private final List<RuleFragment> fragments;

    @Inject
    public RuleFragmentService(FunctionRegistry functionRegistry) {
        this.fragments = new ArrayList<>();
        fragments.addAll(
                List.of(
                        RuleFragment.builder()
                                .fragment("hasField($field) && $message.$field==\"$fieldValue\"")
                                .descriptor(FunctionDescriptor.builder()
                                        .name("test_fragment")
                                        .params(ImmutableList.of(
                                                string("field").build(),
                                                string("fieldValue").build()
                                        ))
                                        .returnType(Boolean.class)
                                        .build())
                                .build()
                ));
    }

    public List<RuleFragment> all() {
        return fragments;
    }


}

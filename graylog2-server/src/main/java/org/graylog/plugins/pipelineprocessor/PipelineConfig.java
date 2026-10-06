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
package org.graylog.plugins.pipelineprocessor;

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.documentation.Documentation;
import com.github.joschi.jadconfig.documentation.DocumentationSection;
import com.github.joschi.jadconfig.validators.PositiveIntegerValidator;
import org.graylog2.plugin.PluginConfigBean;

@DocumentationSection(heading = "Pipeline configuration", description = "")
public class PipelineConfig implements PluginConfigBean {

    @Documentation(value = "tbd")
    @Parameter("cached_stageiterators")
    private boolean cachedStageIterators = true;

    @Documentation("""
            Controls how often Graylog records pipeline rule debug timer samples.
            The default, 100, records roughly one out of every 100 invocations.
            Use 1 to record every invocation.
            Use the same value on every node. Changes require a JVM restart.
            """)
    @Parameter(value = "rule_metrics_sample_rate", validators = PositiveIntegerValidator.class)
    private int ruleMetricsSampleRate = 100;

    public int getRuleMetricsSampleRate() {
        return ruleMetricsSampleRate;
    }
}

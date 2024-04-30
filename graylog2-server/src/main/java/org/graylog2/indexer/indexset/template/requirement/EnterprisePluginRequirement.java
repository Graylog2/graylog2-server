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
package org.graylog2.indexer.indexset.template.requirement;

import org.graylog2.datatiering.fallback.FallbackDataTieringConfig;
import org.graylog2.indexer.indexset.template.IndexSetTemplate;

public class EnterprisePluginRequirement implements IndexSetTemplateRequirement {

    public static final String TEXT = "This template requires Graylog Enterprise.";

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public Result check(IndexSetTemplate indexSetTemplate) {
        if (indexSetTemplate.indexSetConfig().dataTiering() instanceof FallbackDataTieringConfig) {
            return new Result(false, TEXT);
        }
        return new Result(true, "");
    }

}

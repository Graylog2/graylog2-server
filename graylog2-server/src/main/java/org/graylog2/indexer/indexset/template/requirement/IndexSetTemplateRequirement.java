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

import org.graylog2.indexer.indexset.template.IndexSetTemplate;

public interface IndexSetTemplateRequirement {

    /**
     * The priority determines the order in which a requirement check is executed in order to display the most relevant check results first.
     */
    int priority();

    Result check(IndexSetTemplate indexSetTemplate);

    record Result(boolean fulfilled, String reason) {

    }
}

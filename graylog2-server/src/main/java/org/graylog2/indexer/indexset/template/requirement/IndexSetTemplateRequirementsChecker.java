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

import jakarta.inject.Inject;
import org.graylog2.indexer.indexset.template.IndexSetTemplate;

import java.util.Comparator;
import java.util.Set;

import static org.graylog2.indexer.indexset.template.requirement.IndexSetTemplateRequirement.Result;

public class IndexSetTemplateRequirementsChecker {

    Set<IndexSetTemplateRequirement> indexSetTemplateRequirements;

    @Inject
    public IndexSetTemplateRequirementsChecker(Set<IndexSetTemplateRequirement> indexSetTemplateRequirements) {
        this.indexSetTemplateRequirements = indexSetTemplateRequirements;
    }

    public Result check(IndexSetTemplate indexSetTemplate) {
        return indexSetTemplateRequirements.stream()
                .sorted(Comparator.comparing(IndexSetTemplateRequirement::priority))
                .map(indexSetTemplateRequirement -> indexSetTemplateRequirement.check(indexSetTemplate))
                .filter(result -> !result.fulfilled())
                .findFirst()
                .orElse(new Result(true, ""));
    }
}

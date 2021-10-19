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
package org.graylog2.indexer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * This exception, when thrown by an implementation of {@link IndexTemplateProvider},
 * indicates that index template is not required for the current index rotation cycle
 *
 * It might be useful in the following scenarios:
 *      1) index template is managed externally
 *      2) index template cannot be resolved at the moment
 *      and it's acceptable to proceed with an already
 *      existing template in Elasticsearch
 */
public class IgnoreIndexTemplate extends RuntimeException {

    private final boolean failOnMissingTemplate;
    private final String indexTemplateName;

    /**
     * @param failOnMissingTemplate indicates whether the index rotation cycle should fail
     *                              if this template has not been found in Elasticsearch
     * @param reason                indicates the reason for which the index template
     *                              cannot be resolved
     */
    public IgnoreIndexTemplate(boolean failOnMissingTemplate,
                               @Nonnull String reason,
                               @Nonnull String indexPrefix,
                               @Nonnull String indexTemplateName,
                               @Nullable String indexTemplateType) {
        super(f("Ignoring index template with name '%s' and type '%s' (index prefix = '%s'). Reason: %s",
                indexTemplateName, indexTemplateType, indexPrefix, reason));
        this.failOnMissingTemplate = failOnMissingTemplate;
        this.indexTemplateName = indexTemplateName;
    }

    public boolean isFailOnMissingTemplate() {
        return failOnMissingTemplate;
    }

    public String getIndexTemplateName() {
        return indexTemplateName;
    }
}

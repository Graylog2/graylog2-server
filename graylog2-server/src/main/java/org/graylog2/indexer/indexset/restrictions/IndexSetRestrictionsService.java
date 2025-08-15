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
package org.graylog2.indexer.indexset.restrictions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.ParseContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.template.IndexSetDefaultTemplateService;
import org.graylog2.indexer.indexset.template.IndexSetTemplate;
import org.graylog2.indexer.indexset.template.IndexSetTemplateConfig;
import org.graylog2.indexer.indexset.template.IndexSetTemplateService;
import org.graylog2.rest.resources.system.indexer.requests.IndexSetCreationRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class IndexSetRestrictionsService {

    private final IndexSetTemplateService templateService;
    private final IndexSetDefaultTemplateService indexSetDefaultTemplateService;
    private final ObjectMapper objectMapper;
    private final ParseContext parseContext;

    @Inject
    public IndexSetRestrictionsService(IndexSetTemplateService templateService,
                                       IndexSetDefaultTemplateService indexSetDefaultTemplateService,
                                       ObjectMapper objectMapper) {
        this.templateService = templateService;
        this.indexSetDefaultTemplateService = indexSetDefaultTemplateService;
        this.objectMapper = objectMapper;
        parseContext = JsonPath.using(Configuration.defaultConfiguration()
                .addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL));
    }


    public IndexSetConfig createIndexSetConfig(IndexSetCreationRequest creationRequest) {
        IndexSetTemplateConfig indexSetTemplateConfig = Optional.ofNullable(creationRequest.indexSetTemplateId())
                .flatMap(templateService::get)
                .map(IndexSetTemplate::indexSetConfig)
                .orElse(indexSetDefaultTemplateService.getOrCreateDefaultConfig());

        Set<IndexSetFieldRestriction> indexSetFieldRestrictions = indexSetTemplateConfig.fieldRestrictions();

        if (indexSetFieldRestrictions != null && !indexSetFieldRestrictions.isEmpty()) {
            DocumentContext requestContext = createDocument(creationRequest);
            DocumentContext templateContext = createDocument(indexSetTemplateConfig);
            List<String> invalidFields = new ArrayList<>();
            for (IndexSetFieldRestriction r : indexSetFieldRestrictions) {
                if (r instanceof FieldComparator comparator) {
                    if (!comparator.compare(requestContext, templateContext)) {
                        invalidFields.add(r.fieldName());
                    }
                }
            }
            if (!invalidFields.isEmpty()) {
                throw new BadRequestException("The following fields %s are immutable and cannot be changed!".formatted(invalidFields));
            }
        }

        return creationRequest.toIndexSetConfig(true, indexSetTemplateConfig.fieldRestrictions());
    }

    private DocumentContext createDocument(Object o) {
        try {
            return parseContext.parse(objectMapper.writeValueAsString(o));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}

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
import jakarta.ws.rs.ForbiddenException;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.template.IndexSetDefaultTemplateService;
import org.graylog2.indexer.indexset.template.IndexSetTemplate;
import org.graylog2.indexer.indexset.template.IndexSetTemplateConfig;
import org.graylog2.indexer.indexset.template.IndexSetTemplateService;
import org.graylog2.rest.resources.system.indexer.requests.IndexSetCreationRequest;
import org.graylog2.rest.resources.system.indexer.requests.IndexSetUpdateRequest;
import org.graylog2.shared.security.RestPermissions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.graylog2.indexer.indexset.fields.FieldRestrictionsField.FIELD_RESTRICTIONS;

public class IndexSetRestrictionsService {

    public static final String FIELD_RESTRICTIONS_PATH = "$." + FIELD_RESTRICTIONS;
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


    public IndexSetConfig createIndexSetConfig(IndexSetCreationRequest creationRequest, boolean skipRestrictionCheck) {
        IndexSetTemplateConfig indexSetTemplateConfig = Optional.ofNullable(creationRequest.indexSetTemplateId())
                .flatMap(templateService::get)
                .map(IndexSetTemplate::indexSetConfig)
                .orElse(indexSetDefaultTemplateService.getOrCreateDefaultConfig());

        if(!skipRestrictionCheck) {
            checkRestrictions(indexSetTemplateConfig.fieldRestrictions(), doc(creationRequest), doc(indexSetTemplateConfig));
        }

        return creationRequest.toIndexSetConfig(true, indexSetTemplateConfig.fieldRestrictions());
    }

    public IndexSetConfig updateIndexSetConfig(IndexSetUpdateRequest updateRequest,
                                               IndexSetConfig oldConfig,
                                               boolean skipRestrictionCheck) {
        if (!skipRestrictionCheck) {
            DocumentContext doc1 = doc(updateRequest);
            DocumentContext doc2 = doc(oldConfig);
            if (!Objects.equals(doc1.read(FIELD_RESTRICTIONS_PATH), doc2.read(FIELD_RESTRICTIONS_PATH))) {
                throw new ForbiddenException("Missing permission %s to change field %s!".formatted(
                        RestPermissions.INDEXSETS_FIELD_RESTRICTIONS_EDIT, FIELD_RESTRICTIONS));
            }
            checkRestrictions(oldConfig.fieldRestrictions(), doc1, doc2);
        }
        return updateRequest.toIndexSetConfig(oldConfig);
    }

    private void checkRestrictions(Map<String, IndexSetFieldRestriction> indexSetFieldRestrictions,
                                   DocumentContext doc1,
                                   DocumentContext doc2) {

        if (indexSetFieldRestrictions != null && !indexSetFieldRestrictions.isEmpty()) {
            List<String> invalidFields = new ArrayList<>();

            for (Map.Entry<String, IndexSetFieldRestriction> entry : indexSetFieldRestrictions.entrySet()) {
                IndexSetFieldRestriction r = entry.getValue();
                if (r instanceof FieldRestrictionValidator validator && !validator.validate(entry.getKey(), doc1, doc2)) {
                    invalidFields.add(entry.getKey());
                }
            }

            if (!invalidFields.isEmpty()) {
                throw new BadRequestException("The following fields %s violated defined restrictions!".formatted(invalidFields));
            }
        }
    }

    private DocumentContext doc(Object o) {
        try {
            return parseContext.parse(objectMapper.writeValueAsString(o));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}

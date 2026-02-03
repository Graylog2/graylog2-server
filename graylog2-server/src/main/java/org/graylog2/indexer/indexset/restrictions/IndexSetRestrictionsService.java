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

import jakarta.inject.Inject;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.template.IndexSetDefaultTemplateService;
import org.graylog2.indexer.indexset.template.IndexSetTemplate;
import org.graylog2.indexer.indexset.template.IndexSetTemplateConfig;
import org.graylog2.indexer.indexset.template.IndexSetTemplateService;
import org.graylog2.rest.resources.system.indexer.requests.IndexSetCreationRequest;
import org.graylog2.rest.resources.system.indexer.requests.IndexSetUpdateRequest;

import java.util.Optional;

public class IndexSetRestrictionsService {

    private final IndexSetTemplateService templateService;
    private final IndexSetDefaultTemplateService indexSetDefaultTemplateService;
    private final IndexSetConfigTransformer indexSetConfigTransformer;
    private final FieldRestrictionService fieldRestrictionService;


    @Inject
    public IndexSetRestrictionsService(IndexSetTemplateService templateService,
                                       IndexSetDefaultTemplateService indexSetDefaultTemplateService,
                                       IndexSetConfigTransformer indexSetConfigTransformer,
                                       FieldRestrictionService fieldRestrictionService) {
        this.templateService = templateService;
        this.indexSetDefaultTemplateService = indexSetDefaultTemplateService;
        this.indexSetConfigTransformer = indexSetConfigTransformer;
        this.fieldRestrictionService = fieldRestrictionService;
    }


    public IndexSetConfig createIndexSetConfig(IndexSetCreationRequest creationRequest, boolean skipRestrictionCheck) {
        IndexSetTemplateConfig indexSetTemplateConfig = indexSetConfigTransformer.transform(
                Optional.ofNullable(creationRequest.indexSetTemplateId())
                        .flatMap(templateService::get)
                        .map(IndexSetTemplate::indexSetConfig)
                        .orElse(indexSetDefaultTemplateService.getOrCreateDefaultConfig()));

        final IndexSetConfig newConfig = creationRequest.toIndexSetConfig(true, indexSetTemplateConfig.fieldRestrictions());
        if (!skipRestrictionCheck) {
            fieldRestrictionService.checkRestrictions(indexSetTemplateConfig.fieldRestrictions(), newConfig, indexSetTemplateConfig);
        }
        return newConfig;
    }

    public IndexSetConfig updateIndexSetConfig(IndexSetUpdateRequest updateRequest,
                                               IndexSetConfig oldConfig,
                                               boolean skipRestrictionCheck) {
        final IndexSetConfig newConfig = updateRequest.toIndexSetConfig(oldConfig);
        if (!skipRestrictionCheck) {
            fieldRestrictionService.checkRestrictions(oldConfig.fieldRestrictions(), newConfig, indexSetConfigTransformer.transform(oldConfig));
        }
        return newConfig;
    }

}

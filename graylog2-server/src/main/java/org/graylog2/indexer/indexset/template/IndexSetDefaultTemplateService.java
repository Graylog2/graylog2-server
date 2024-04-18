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
package org.graylog2.indexer.indexset.template;

import com.google.common.collect.ImmutableMap;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import org.graylog2.audit.AuditActor;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.configuration.IndexSetDefaultTemplateConfigFactory;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static org.graylog2.audit.AuditEventTypes.INDEX_SET_DEFAULT_TEMPLATE_UPDATE;

public class IndexSetDefaultTemplateService {

    private static final Logger LOG = LoggerFactory.getLogger(IndexSetDefaultTemplateService.class);

    private final ClusterConfigService clusterConfigService;
    private final IndexSetTemplateService indexSetTemplateService;
    private final IndexSetDefaultTemplateConfigFactory indexSetDefaultTemplateConfigFactory;
    private final AuditEventSender auditEventSender;

    @Inject
    public IndexSetDefaultTemplateService(ClusterConfigService clusterConfigService,
                                          IndexSetTemplateService indexSetTemplateService,
                                          IndexSetDefaultTemplateConfigFactory indexSetDefaultTemplateConfigFactory,
                                          AuditEventSender auditEventSender) {
        this.clusterConfigService = clusterConfigService;
        this.indexSetTemplateService = indexSetTemplateService;
        this.indexSetDefaultTemplateConfigFactory = indexSetDefaultTemplateConfigFactory;
        this.auditEventSender = auditEventSender;
    }

    public Optional<IndexSetTemplate> getDefaultIndexSetTemplate() {
        return Optional.ofNullable(clusterConfigService.get(IndexSetDefaultTemplate.class))
                .flatMap(indexSetDefaultTemplate -> indexSetTemplateService.get(indexSetDefaultTemplate.id()));
    }

    public IndexSetTemplateConfig createDefaultConfig() {
        return getDefaultIndexSetTemplate()
                .map(IndexSetTemplate::indexSetConfig)
                .orElse(createDefault());
    }

    private IndexSetTemplateConfig createDefault() {
        LOG.debug("Could not find default configuration. Falling back to server configuration values.");
        return indexSetDefaultTemplateConfigFactory.create();
    }

    public IndexSetTemplate createAndSaveDefault(IndexSetTemplate defaultIndexSetTemplate) {
        IndexSetTemplate savedTemplate = indexSetTemplateService.save(defaultIndexSetTemplate);
        clusterConfigService.write(new IndexSetDefaultTemplate(savedTemplate.id()));
        return savedTemplate;
    }

    public void setDefault(@NotNull IndexSetDefaultTemplate defaultTemplate, String userName) throws NotFoundException {
        Optional<IndexSetTemplate> indexSetTemplate = indexSetTemplateService.get(defaultTemplate.id());
        if (indexSetTemplate.isPresent()) {
            clusterConfigService.write(defaultTemplate);
            auditEventSender.success(AuditActor.user(userName), INDEX_SET_DEFAULT_TEMPLATE_UPDATE, ImmutableMap.of(
                    "template_id", indexSetTemplate.get().id(),
                    "template_title", indexSetTemplate.get().title()
            ));
        } else {
            throw new NotFoundException("Index template with id <%s> doesn't exist!".formatted(defaultTemplate.id()));
        }
    }
}

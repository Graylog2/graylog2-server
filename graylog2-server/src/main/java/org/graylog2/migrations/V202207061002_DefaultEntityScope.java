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
package org.graylog2.migrations;

import org.graylog2.entityscope.EntityScope;
import org.graylog2.entityscope.EntityScopeDbService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.Optional;

public class V202207061002_DefaultEntityScope extends Migration {

    private static final Logger LOG = LoggerFactory.getLogger(V202207061002_DefaultEntityScope.class.getSimpleName());
    private static final EntityScope DEFAULT_SCOPE = EntityScope.Builder.createModifiable("default_entity_scope", "Default Entity Scope");

    private final EntityScopeDbService dbService;

    @Inject
    public V202207061002_DefaultEntityScope(EntityScopeDbService dbService) {
        this.dbService = dbService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2022-07-06T10:10:00Z");
    }

    @Override
    public void upgrade() {

        Optional<EntityScope> optCurrent = dbService.findByName(DEFAULT_SCOPE.title());
        if (!optCurrent.isPresent()) {
            EntityScope saved = dbService.save(DEFAULT_SCOPE);
            LOG.debug("Created Default Entity Scope: {} - {}", saved.id(), saved.title());
        }

    }
}

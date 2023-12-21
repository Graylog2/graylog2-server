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
package org.graylog.plugins.views.search.rest.scriptingapi.response.decorators;

import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.rest.scriptingapi.request.RequestedField;
import org.graylog2.plugin.Message;
import org.graylog2.rest.resources.system.contentpacks.titles.EntityTitleService;
import org.graylog2.rest.resources.system.contentpacks.titles.model.EntitiesTitleResponse;
import org.graylog2.rest.resources.system.contentpacks.titles.model.EntityIdentifier;
import org.graylog2.rest.resources.system.contentpacks.titles.model.EntityTitleRequest;
import org.graylog2.rest.resources.system.contentpacks.titles.model.EntityTitleResponse;

import javax.annotation.Nullable;

import jakarta.inject.Inject;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class TitleDecorator implements FieldDecorator {

    private final EntityTitleService entityTitleService;


    private static final Map<String, String> FIELD_ENTITY_MAPPER = Map.of(
            Message.FIELD_STREAMS, "streams",
            Message.FIELD_GL2_SOURCE_INPUT, "inputs"
    );

    @Inject
    public TitleDecorator(EntityTitleService entityTitleService) {
        this.entityTitleService = entityTitleService;
    }

    @Override
    public boolean accept(RequestedField field) {
        return FIELD_ENTITY_MAPPER.containsKey(field.name()) && acceptsDecorator(field.decorator());
    }

    private boolean acceptsDecorator(@Nullable String decorator) {
        return decorator == null || decorator.equals("title");
    }

    @Override
    public Object decorate(RequestedField field, Object value, SearchUser searchUser) {

        final List<String> ids = parseIDs(value);
        final EntityTitleRequest req = ids.stream()
                .map(id -> new EntityIdentifier(id, FIELD_ENTITY_MAPPER.get(field.name())))
                .collect(Collectors.collectingAndThen(Collectors.toList(), EntityTitleRequest::new));

        final EntitiesTitleResponse response = entityTitleService.getTitles(req, searchUser);
        return extractTitles(ids, response.entities()).stream()
                .collect(Collectors.collectingAndThen(Collectors.toList(), titles -> value instanceof Collection<?> ? titles : unwrapIfSingleResult(titles)));
    }

    private List<String> extractTitles(List<String> ids, Set<EntityTitleResponse> entities) {
        return ids.stream()
                .map(id -> extractTitle(entities, id))
                .collect(Collectors.toList());
    }

    private static String extractTitle(Set<EntityTitleResponse> entities, String id) {
        return entities.stream().
                filter(e -> Objects.equals(id, e.id()))
                .findFirst()
                .map(EntityTitleResponse::title)
                .orElse(id);
    }

    /**
     * We need to unwrap single results, otherwise they'll appear in the output always as array, which is neither
     * backwards compatible nor expected and causes only troubles. If the results are really a list of more items,
     * we keep them and forward them as they are.
     */
    private Object unwrapIfSingleResult(List<String> titles) {
        if (titles.size() == 1) {
            return titles.iterator().next();
        } else {
            return titles;
        }
    }

    private List<String> parseIDs(Object value) {
        if (value instanceof Collection<?> col) {
            return col.stream().map(Object::toString).collect(Collectors.toList());
        } else {
            return Collections.singletonList(value.toString());
        }
    }
}

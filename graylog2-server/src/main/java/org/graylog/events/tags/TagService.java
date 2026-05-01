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
package org.graylog.events.tags;

import jakarta.inject.Inject;
import org.graylog2.database.nameonly.NameOnlyEntityService;

public class TagService extends NameOnlyEntityService<Tag, Tag.Builder> {

    @Inject
    public TagService(DBTagService dbTagService) {
        super(dbTagService);
    }

    @Override
    protected Tag buildEntity(String value) {
        return Tag.builder().value(value).build();
    }

    @Override
    protected String entityValue(Tag entity) {
        return entity.value();
    }

    @Override
    protected String entityLabel() {
        return "Tag";
    }
}

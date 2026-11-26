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
package org.graylog2.system.activities;

import jakarta.annotation.Nullable;
import org.graylog2.database.PaginatedList;
import org.graylog2.plugin.database.PersistedService;
import org.joda.time.DateTime;

import java.util.List;
import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public interface SystemMessageService extends PersistedService {
    List<SystemMessage> all(int page);
    long totalCount();

    /**
     * The messages whose content or caller contains the query, ignoring case, newest first.
     *
     * @param query plain text, not a search query; empty matches every message
     * @param since only messages at or after this time, if given
     * @param page  the page of 30 messages, starting at 1
     */
    PaginatedList<SystemMessage> find(String query, @Nullable DateTime since, int page);
    SystemMessage create(Map<String, Object> fields);
}

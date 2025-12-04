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
package org.graylog.storage.opensearch3;

import jakarta.inject.Inject;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.indexer.results.ResultMessageFactory;
import org.opensearch.client.opensearch.core.search.Hit;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OS2ResultMessageFactory {

    private final ResultMessageFactory messageFactory;

    @Inject
    public OS2ResultMessageFactory(ResultMessageFactory messageFactory) {
        this.messageFactory = messageFactory;
    }

    public ResultMessage fromSearchHit(Hit<Map> hit) {
        return messageFactory.parseFromSource(hit.id(), hit.index(), hit.source(), hit.highlight());
    }

}

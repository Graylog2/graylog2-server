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
package org.graylog2.indexer.messages;

import org.graylog2.indexer.results.ResultMessage;

import java.io.IOException;
import java.util.List;

public interface MessagesAdapter {
    ResultMessage get(String messageId, String index) throws IOException, DocumentNotFoundException;

    List<String> analyze(String toAnalyze, String index, String analyzer) throws IOException;

    List<Messages.IndexingError> bulkIndex(final List<IndexingRequest> messageList) throws IOException;
}

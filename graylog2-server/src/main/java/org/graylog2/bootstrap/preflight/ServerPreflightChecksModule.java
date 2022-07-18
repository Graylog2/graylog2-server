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
package org.graylog2.bootstrap.preflight;

import org.graylog2.audit.AuditEventSender;
import org.graylog2.audit.NullAuditEventSender;
import org.graylog2.plugin.inject.Graylog2Module;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.bindings.providers.NodeIdProvider;
import org.graylog2.storage.providers.ElasticsearchVersionProvider;

public class ServerPreflightChecksModule extends Graylog2Module {

    @Override
    protected void configure() {
        bind(ElasticsearchVersionProvider.class).asEagerSingleton();
        bind(NodeId.class).toProvider(NodeIdProvider.class);
        bind(AuditEventSender.class).to(NullAuditEventSender.class);

        addPreflightCheck(MongoDBPreflightCheck.class);
        addPreflightCheck(SearchDbPreflightCheck.class);
        addPreflightCheck(DiskJournalPreflightCheck.class);
    }
}

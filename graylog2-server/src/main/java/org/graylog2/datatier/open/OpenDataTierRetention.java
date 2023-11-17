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
package org.graylog2.datatier.open;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import org.graylog2.audit.AuditActor;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.datatier.common.DataTierRetention;
import org.graylog2.datatier.common.tier.HotTierConfig;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.plugin.system.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.graylog2.audit.AuditEventTypes.ES_INDEX_RETENTION_DELETE;

public class OpenDataTierRetention {

    private static final Logger LOG = LoggerFactory.getLogger(OpenDataTierRetention.class);

    private final DataTierRetention dataTierRetention;
    private final Indices indices;

    private final AuditEventSender auditEventSender;

    private final NodeId nodeId;

    @Inject
    public OpenDataTierRetention(DataTierRetention dataTierRetention,
                                 Indices indices,
                                 AuditEventSender auditEventSender,
                                 NodeId nodeId) {
        this.dataTierRetention = dataTierRetention;
        this.indices = indices;
        this.auditEventSender = auditEventSender;
        this.nodeId = nodeId;
    }

    public void retain(IndexSet indexSet, HotTierConfig config) {
        dataTierRetention.retain(indexSet, config, this::retain);
    }

    private void retain(List<String> indexNames, IndexSet indexSet) {
        indexNames.forEach(indexName -> {
            final Stopwatch sw = Stopwatch.createStarted();

            indices.delete(indexName);
            auditEventSender.success(AuditActor.system(nodeId), ES_INDEX_RETENTION_DELETE, ImmutableMap.of(
                    "index_name", indexName,
                    "data_tier_retention", OpenDataTiersConfig.TYPE_OPEN
            ));

            LOG.info("Finished {} data tier retention for index <{}> in {}ms.", OpenDataTiersConfig.TYPE_OPEN, indexName,
                    sw.stop().elapsed(TimeUnit.MILLISECONDS));
        });
    }
}

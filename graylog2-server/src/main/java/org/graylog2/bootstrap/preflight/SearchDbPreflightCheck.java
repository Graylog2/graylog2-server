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

import org.graylog2.shared.utilities.StringUtils;
import org.graylog2.storage.SearchVersion;
import org.graylog2.storage.versionprobe.ElasticsearchProbeException;
import org.graylog2.storage.versionprobe.VersionProbe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.net.URI;
import java.util.List;

import static org.graylog2.configuration.validators.ElasticsearchVersionValidator.SUPPORTED_ES_VERSIONS;

public class SearchDbPreflightCheck implements PreflightCheck {
    private static final Logger LOG = LoggerFactory.getLogger(SearchDbPreflightCheck.class);

    private final VersionProbe elasticVersionProbe;
    private final List<URI> elasticsearchHosts;

    @Inject
    public SearchDbPreflightCheck(
            VersionProbe elasticVersionProbe,
            @Named("elasticsearch_hosts") List<URI> elasticsearchHosts) {
        this.elasticVersionProbe = elasticVersionProbe;
        this.elasticsearchHosts = elasticsearchHosts;
    }

    @Override
    public void runCheck() throws PreflightCheckException {
        try {
            final SearchVersion searchVersion = elasticVersionProbe.probe(elasticsearchHosts)
                    .orElseThrow(() -> new PreflightCheckException("Could not get Elasticsearch version"));

            if (SUPPORTED_ES_VERSIONS.stream().noneMatch(searchVersion::satisfies)) {
                throw new PreflightCheckException(StringUtils.f("Unsupported (Elastic/Open)Search version <%s>. Supported versions: <%s>",
                        searchVersion, SUPPORTED_ES_VERSIONS));
            }

            LOG.info("Connected to (Elastic/Open)Search version <{}>", searchVersion);
        } catch (ElasticsearchProbeException e) {
            throw new PreflightCheckException(e);
        }
    }
}

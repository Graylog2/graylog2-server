/*
 * Copyright 2012-2014 TORCH GmbH
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.graylog2.initializers;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Singleton;
import org.graylog2.indexer.Indexer;
import org.graylog2.plugin.Tools;

import javax.inject.Inject;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
@Singleton
public class IndexerSetupService extends AbstractIdleService {
    private final Indexer indexer;
    private final BufferSynchronizerService bufferSynchronizerService;

    @Inject
    public IndexerSetupService(Indexer indexer, BufferSynchronizerService bufferSynchronizerService) {
        this.indexer = indexer;
        this.bufferSynchronizerService = bufferSynchronizerService;
    }

    @Override
    protected void startUp() throws Exception {
        Tools.silenceUncaughtExceptionsInThisThread();
        try {
            indexer.start();
        } catch (Exception e) {
            bufferSynchronizerService.setIndexerUnavailable();
            throw e;
        }
    }

    @Override
    protected void shutDown() throws Exception {
        // Properly close ElasticSearch node.
        indexer.getNode().close();
    }
}

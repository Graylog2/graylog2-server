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

package org.graylog2.shared.initializers;

import com.google.common.util.concurrent.AbstractIdleService;
import org.graylog2.inputs.gelf.gelf.GELFChunkManager;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
@Singleton
public class GelfChunkManagerService extends AbstractIdleService {
    private GELFChunkManager gelfChunkManager;

    @Inject
    public GelfChunkManagerService(GELFChunkManager gelfChunkManager) {
        this.gelfChunkManager = gelfChunkManager;
    }

    @Override
    protected void startUp() throws Exception {
        gelfChunkManager.start();
    }

    @Override
    protected void shutDown() throws Exception {
    }
}

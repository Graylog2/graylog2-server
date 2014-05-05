/*
 * Copyright 2013 TORCH UG
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
package org.graylog2.restclient.models;

import org.graylog2.restclient.models.api.responses.BufferSummaryResponse;
import org.graylog2.restclient.models.api.responses.BuffersResponse;
import org.graylog2.restclient.models.api.responses.MasterCacheSummaryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class BufferInfo {
    private static final Logger log = LoggerFactory.getLogger(BufferInfo.class);

    private final BufferSummaryResponse inputBuffer;
    private final BufferSummaryResponse outputBuffer;
    private final MasterCacheSummaryResponse inputMasterCache;
    private final MasterCacheSummaryResponse outputMasterCache;

    public BufferInfo(BuffersResponse br) {
        inputBuffer = br.buffers.get("input");
        outputBuffer = br.buffers.get("output");
        inputMasterCache = br.masterCaches.get("input");
        outputMasterCache = br.masterCaches.get("output");
    }

    public BufferSummaryResponse getInputBuffer() {
        return inputBuffer;
    }

    public BufferSummaryResponse getOutputBuffer() {
        return outputBuffer;
    }

    public MasterCacheSummaryResponse getInputMasterCache() {
        return inputMasterCache;
    }

    public MasterCacheSummaryResponse getOutputMasterCache() {
        return outputMasterCache;
    }
}

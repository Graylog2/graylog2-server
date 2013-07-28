/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
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
 *
 */
package models;

import lib.APIException;
import lib.Api;
import models.api.responses.BufferSummaryResponse;
import models.api.responses.BuffersResponse;
import models.api.responses.MasterCacheSummaryResponse;

import java.io.IOException;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class BufferInfo {

    public final BufferSummaryResponse inputBuffer;
    public final BufferSummaryResponse outputBuffer;
    public final MasterCacheSummaryResponse inputMasterCache;
    public final MasterCacheSummaryResponse outputMasterCache;

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

    public static BufferInfo ofNode(Node node) throws IOException, APIException {
        return new BufferInfo(Api.get(node, "system/buffers", BuffersResponse.class));
    }

}

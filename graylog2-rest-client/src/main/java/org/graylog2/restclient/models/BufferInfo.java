/**
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

import com.google.common.collect.Maps;
import org.graylog2.restclient.models.api.responses.BufferSummaryResponse;
import org.graylog2.restclient.models.api.responses.BuffersResponse;
import org.graylog2.restclient.models.api.responses.MasterCacheSummaryResponse;

import static org.graylog2.restclient.lib.Tools.firstNonNull;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class BufferInfo {
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
        return firstNonNull(BufferSummaryResponse.EMPTY, inputBuffer);
    }

    public BufferSummaryResponse getOutputBuffer() {
        return firstNonNull(BufferSummaryResponse.EMPTY, outputBuffer);
    }

    public MasterCacheSummaryResponse getInputMasterCache() {
        return firstNonNull(MasterCacheSummaryResponse.EMPTY, inputMasterCache);
    }

    public MasterCacheSummaryResponse getOutputMasterCache() {
        return firstNonNull(MasterCacheSummaryResponse.EMPTY, outputMasterCache);
    }

    public static BufferInfo buildEmpty() {
        final BuffersResponse response = new BuffersResponse();

        response.buffers = Maps.newHashMap();
        response.masterCaches = Maps.newHashMap();

        return new BufferInfo(response);
    }
}

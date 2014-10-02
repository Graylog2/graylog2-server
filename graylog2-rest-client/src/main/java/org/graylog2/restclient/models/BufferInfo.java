/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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

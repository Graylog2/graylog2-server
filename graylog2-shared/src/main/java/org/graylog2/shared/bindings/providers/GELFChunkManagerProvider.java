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
package org.graylog2.shared.bindings.providers;

import com.codahale.metrics.MetricRegistry;
import org.graylog2.inputs.gelf.gelf.GELFChunkManager;
import org.graylog2.shared.buffers.ProcessBuffer;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class GELFChunkManagerProvider implements Provider<GELFChunkManager> {
    private final MetricRegistry metricRegistry;
    private final ProcessBuffer processBuffer;

    @Inject
    public GELFChunkManagerProvider(MetricRegistry metricRegistry,
                                    ProcessBuffer processBuffer) {
        this.metricRegistry = metricRegistry;
        this.processBuffer = processBuffer;
    }

    @Override
    public GELFChunkManager get() {
        final GELFChunkManager gelfChunkManager = new GELFChunkManager(metricRegistry, processBuffer);
        gelfChunkManager.setName("gelf-chunk-manager");
        return gelfChunkManager;
    }
}

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
package org.graylog2.initializers;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import org.graylog2.Configuration;
import org.graylog2.buffers.OutputBuffer;
import org.graylog2.buffers.processors.ServerProcessBufferProcessor;
import org.graylog2.shared.buffers.ProcessBuffer;
import org.graylog2.shared.buffers.processors.ProcessBufferProcessor;

import javax.inject.Singleton;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
@Singleton
public class ProcessBufferService extends AbstractIdleService {
    private final Configuration configuration;
    private final ServerProcessBufferProcessor.Factory processBufferProcessorFactory;
    private final OutputBuffer outputBuffer;
    private final ProcessBuffer processBuffer;

    @Inject
    public ProcessBufferService(Configuration configuration,
                                ServerProcessBufferProcessor.Factory processBufferProcessorFactory,
                                OutputBuffer outputBuffer,
                                ProcessBuffer processBuffer) {
        this.configuration = configuration;
        this.processBufferProcessorFactory = processBufferProcessorFactory;
        this.outputBuffer = outputBuffer;
        this.processBuffer = processBuffer;

        outputBuffer.initialize();
    }

    @Override
    protected void startUp() throws Exception {
        int processBufferProcessorCount = configuration.getProcessBufferProcessors();

        ProcessBufferProcessor[] processors = new ProcessBufferProcessor[processBufferProcessorCount];

        for (int i = 0; i < processBufferProcessorCount; i++) {
            processors[i] = processBufferProcessorFactory.create(outputBuffer, i, processBufferProcessorCount);
        }

        processBuffer.initialize(processors, configuration.getRingSize(),
                configuration.getProcessorWaitStrategy(),
                configuration.getProcessBufferProcessors()
        );

    }

    @Override
    protected void shutDown() throws Exception {
    }
}

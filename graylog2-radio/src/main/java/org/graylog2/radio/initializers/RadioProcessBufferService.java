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
package org.graylog2.radio.initializers;

import com.google.common.util.concurrent.AbstractIdleService;
import org.graylog2.radio.Configuration;
import org.graylog2.radio.buffers.processors.RadioProcessBufferProcessor;
import org.graylog2.shared.buffers.ProcessBuffer;
import org.graylog2.shared.buffers.processors.ProcessBufferProcessor;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
@Singleton
public class RadioProcessBufferService extends AbstractIdleService {
    private final RadioProcessBufferProcessor.Factory processBufferProcessorFactory;
    private final Configuration configuration;
    private final ProcessBuffer processBuffer;

    @Inject
    public RadioProcessBufferService(RadioProcessBufferProcessor.Factory processBufferProcessorFactory,
                                     Configuration configuration,
                                     ProcessBuffer processBuffer) {
        this.processBufferProcessorFactory = processBufferProcessorFactory;
        this.configuration = configuration;
        this.processBuffer = processBuffer;
    }

    @Override
    protected void startUp() throws Exception {
        int processBufferProcessorCount = configuration.getProcessBufferProcessors();

        ProcessBufferProcessor[] processors = new ProcessBufferProcessor[processBufferProcessorCount];

        for (int i = 0; i < processBufferProcessorCount; i++) {
            processors[i] = processBufferProcessorFactory.create(i, processBufferProcessorCount);
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

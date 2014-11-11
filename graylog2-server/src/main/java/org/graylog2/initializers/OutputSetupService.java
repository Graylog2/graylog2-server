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

import com.codahale.metrics.InstrumentedExecutorService;
import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.AbstractIdleService;
import org.graylog2.outputs.OutputRegistry;
import org.graylog2.plugin.outputs.MessageOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.Executors;

@Singleton
public class OutputSetupService extends AbstractIdleService {
    private static final Logger LOG = LoggerFactory.getLogger(OutputSetupService.class);

    private final OutputRegistry outputRegistry;

    @Inject
    public OutputSetupService(final OutputRegistry outputRegistry,
                              final BufferSynchronizerService bufferSynchronizerService,
                              final MetricRegistry metricRegistry) {
        this.outputRegistry = outputRegistry;

        // Shutdown after the BufferSynchronizerService has stopped to avoid shutting down outputs too early.
        bufferSynchronizerService.addListener(new Listener() {
            @Override
            public void terminated(State from) {
                OutputSetupService.this.shutDownRunningOutputs();
            }
        }, new InstrumentedExecutorService(Executors.newSingleThreadExecutor(), metricRegistry));
    }

    private void shutDownRunningOutputs() {
        for (MessageOutput output : outputRegistry.getMessageOutputs()) {
            try {
                // TODO: change to debug
                LOG.info("Stopping output {}", output.getClass().getName());
                output.stop();
            } catch (Exception e) {
                LOG.error("Error stopping output", e);
            }
        }
    }

    @Override
    protected void startUp() throws Exception {
        // Outputs are started lazily in the OutputRegistry.
    }

    @Override
    protected void shutDown() throws Exception {
        // Outputs are stopped when the BufferSynchronizerService has stopped. See constructor.
    }
}

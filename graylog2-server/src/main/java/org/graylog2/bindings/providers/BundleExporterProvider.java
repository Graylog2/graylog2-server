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
package org.graylog2.bindings.providers;

import org.graylog2.bundles.BundleExporter;
import org.graylog2.dashboards.DashboardService;
import org.graylog2.inputs.InputService;
import org.graylog2.streams.OutputService;
import org.graylog2.streams.StreamService;

import javax.inject.Inject;
import javax.inject.Provider;

public class BundleExporterProvider implements Provider<BundleExporter> {

    private final InputService inputService;
    private final StreamService streamService;
    private final OutputService outputService;
    private final DashboardService dashboardService;

    @Inject
    public BundleExporterProvider(final InputService inputService,
                                  final StreamService streamService,
                                  final OutputService outputService,
                                  final DashboardService dashboardService) {
        this.inputService = inputService;
        this.streamService = streamService;
        this.outputService = outputService;
        this.dashboardService = dashboardService;
    }

    @Override
    public BundleExporter get() {
        return new BundleExporter(inputService, streamService, outputService, dashboardService);
    }
}

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
package org.graylog2.bindings;

import com.google.common.util.concurrent.Service;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import org.graylog2.initializers.*;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class InitializerBindings extends AbstractModule {
    @Override
    protected void configure() {
        Multibinder<Service> serviceBinder = Multibinder.newSetBinder(binder(), Service.class);
        serviceBinder.addBinding().to(MetricsReporterService.class);
        serviceBinder.addBinding().to(DashboardRegistryService.class);
        serviceBinder.addBinding().to(ProcessBufferService.class);
        serviceBinder.addBinding().to(IndexerSetupService.class);
        serviceBinder.addBinding().to(BufferSynchronizerService.class);
        serviceBinder.addBinding().to(OutputSetupService.class);
    }
}

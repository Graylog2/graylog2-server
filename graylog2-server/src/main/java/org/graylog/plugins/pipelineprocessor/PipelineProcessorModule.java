/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.pipelineprocessor;

import com.google.inject.assistedinject.FactoryModuleBuilder;
import org.graylog.plugins.pipelineprocessor.audit.PipelineProcessorAuditEventTypes;
import org.graylog.plugins.pipelineprocessor.functions.ProcessorFunctionsModule;
import org.graylog.plugins.pipelineprocessor.periodical.LegacyDefaultStreamMigration;
import org.graylog.plugins.pipelineprocessor.processors.PipelineInterpreter;
import org.graylog.plugins.pipelineprocessor.rest.PipelineRestPermissions;
import org.graylog2.plugin.PluginModule;

public class PipelineProcessorModule extends PluginModule {
    @Override
    protected void configure() {
        addPeriodical(LegacyDefaultStreamMigration.class);

        addMessageProcessor(PipelineInterpreter.class, PipelineInterpreter.Descriptor.class);
        addPermissions(PipelineRestPermissions.class);

        registerRestControllerPackage(getClass().getPackage().getName());

        install(new ProcessorFunctionsModule());

        installSearchResponseDecorator(searchResponseDecoratorBinder(),
                PipelineProcessorMessageDecorator.class,
                PipelineProcessorMessageDecorator.Factory.class);

        install(new FactoryModuleBuilder().build(PipelineInterpreter.State.Factory.class));

        addAuditEventTypes(PipelineProcessorAuditEventTypes.class);
    }
}

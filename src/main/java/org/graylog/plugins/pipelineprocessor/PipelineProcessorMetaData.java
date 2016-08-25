/**
 * This file is part of Graylog Pipeline Processor.
 *
 * Graylog Pipeline Processor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog Pipeline Processor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog Pipeline Processor.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.pipelineprocessor;

import com.google.common.collect.Sets;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Version;

import java.net.URI;
import java.util.Set;

/**
 * Implement the PluginMetaData interface here.
 */
public class PipelineProcessorMetaData implements PluginMetaData {
    @Override
    public String getUniqueId() {
        return "org.graylog.plugins.pipelineprocessor.ProcessorPlugin";
    }

    @Override
    public String getName() {
        return "Pipeline Processor Plugin";
    }

    @Override
    public String getAuthor() {
        return "Graylog, Inc";
    }

    @Override
    public URI getURL() {
        return URI.create("https://www.graylog.org/");
    }

    @Override
    public Version getVersion() {
        return new Version(1, 1, 0, "rc.1");
    }

    @Override
    public String getDescription() {
        return "Pluggable pipeline processing framework";
    }

    @Override
    public Version getRequiredVersion() {
        return new Version(2, 1, 0);
    }

    @Override
    public Set<ServerStatus.Capability> getRequiredCapabilities() {
        return Sets.newHashSet(ServerStatus.Capability.SERVER);
    }
}

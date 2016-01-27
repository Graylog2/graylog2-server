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
package org.graylog2.rest.resources.system;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.messageprocessors.MessageProcessorsConfig;
import org.graylog2.messageprocessors.MessageProcessorsConfigWithDescriptors;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.messageprocessors.MessageProcessor;
import org.graylog2.shared.rest.resources.RestResource;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Set;
import java.util.stream.Collectors;

@RequiresAuthentication
@Api(value = "System/MessageProcessors", description = "Manage message processors")
@Path("/system/messageprocessors")
@Produces(MediaType.APPLICATION_JSON)
public class MessageProcessorsResource extends RestResource {
    private final Set<String> processorClassNames;
    private final ClusterConfigService clusterConfigService;
    private final Set<MessageProcessor.Descriptor> processorDescriptors;

    @Inject
    public MessageProcessorsResource(final Set<MessageProcessor.Descriptor> processorDescriptors,
                                     final ClusterConfigService clusterConfigService) {
        this.processorDescriptors = processorDescriptors;
        this.processorClassNames = processorDescriptors.stream()
                .map(MessageProcessor.Descriptor::className)
                .collect(Collectors.toSet());
        this.clusterConfigService = clusterConfigService;
    }


    @GET
    @Timed
    @ApiOperation(value = "Get message processor configuration")
    @Path("config")
    public MessageProcessorsConfigWithDescriptors config() {
        final MessageProcessorsConfig config = clusterConfigService.getOrDefault(MessageProcessorsConfig.class,
                MessageProcessorsConfig.defaultConfig());

        return MessageProcessorsConfigWithDescriptors.fromConfig(config.withProcessors(processorClassNames), processorDescriptors);
    }

    @PUT
    @Timed
    @ApiOperation(value = "Update message processor configuration")
    @Path("config")
    public MessageProcessorsConfigWithDescriptors updateConfig(@ApiParam(name = "config", required = true) final MessageProcessorsConfigWithDescriptors configWithDescriptors) {
        final MessageProcessorsConfig config = configWithDescriptors.toConfig();

        clusterConfigService.write(config.withProcessors(processorClassNames));

        return configWithDescriptors;
    }
}

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
package org.graylog2.shared.rest.resources.system.codecs;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.inputs.codecs.CodecFactory;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.graylog2.rest.models.system.codecs.responses.CodecTypeInfo;
import org.graylog2.shared.rest.resources.RestResource;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Map;
import java.util.stream.Collectors;

@RequiresAuthentication
@Api(value = "System/Codecs/Types", description = "Message codec types of this node")
@Path("/system/codecs/types")
@Produces(MediaType.APPLICATION_JSON)
public class CodecTypesResource extends RestResource {
    private CodecFactory codecFactory;

    @Inject
    public CodecTypesResource(CodecFactory codecFactory) {
        this.codecFactory = codecFactory;
    }

    @GET
    @Timed
    @Path("/all")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get all codec types")
    public Map<String, CodecTypeInfo> getAll() {
        final Map<String, Codec.Factory<? extends Codec>> factories = codecFactory.getFactory();

        return factories
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            final Codec.Factory<? extends Codec> factory = entry.getValue();
                            return CodecTypeInfo.fromConfigurationRequest(entry.getKey(), factory.getDescriptor().getName(), factory.getConfig().getRequestedConfiguration());
                        }
                ));
    }
}

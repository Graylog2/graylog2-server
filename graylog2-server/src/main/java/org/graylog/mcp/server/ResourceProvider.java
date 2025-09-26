/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.mcp.server;

import io.modelcontextprotocol.spec.McpSchema;
import jakarta.annotation.Nullable;
import org.glassfish.jersey.uri.UriTemplate;
import org.graylog2.database.NotFoundException;

import java.net.URI;
import java.util.List;

/**
 * Subclasses provide MCP resources.
 *
 * Graylog typically has database or in-memory services for its entities, which we expose as
 * resources, and most of the time we need to load and paginate over them.
 * Thus, the typical pattern is to have a shallow wrapper around the service, which checks for authorization
 * and does field projection or otherwise chooses the appropriate level of detail and shape for the
 * entity exposed.
 */
public abstract class ResourceProvider {

    /**
     * The <a href="https://modelcontextprotocol.io/specification/2025-06-18/server/resources#resource-templates">resource template</a>
     * for resources provided by this class.
     *
     * @return the resource template information
     */
    public abstract Template resourceTemplate();

    /**
     * Produce the resource identified by the given URI.
     *
     * @param uri a {@link org.graylog.grn.GRN GRN}
     * @return the loaded resource object
     * @throws NotFoundException if Graylog cannot find this resource
     */
    public abstract McpSchema.Resource read(URI uri) throws NotFoundException;

    /**
     * Provide a list of available resources
     *
     * @param cursor a nullable cursor to provide pagination information
     * @param pageSize number of items to return
     */
    public abstract List<McpSchema.Resource> list(@Nullable PaginatedList.Cursor cursor, @Nullable Integer pageSize);

    /**
     * Templates are used to guide MCP clients, <a href="https://modelcontextprotocol.io/specification/2025-06-18/server/resources#resource-templates">see the spec.</a>
     *
     * @param uriTemplate the URI template, for us these are GRNs
     * @param name        the short-form name of the resources described
     * @param title       the human-readable title of the resource template
     * @param description the long-form description of resources of this type
     * @param contentType the MIME type
     */
    public record Template(UriTemplate uriTemplate, String name, String title, String description,
                           String contentType) {}

}

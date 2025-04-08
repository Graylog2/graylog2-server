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
package org.graylog2.storage.versionprobe;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/")
public class OpensearchHealthResponse {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String get() {
        return """
                {
                  "name" : "tdvorak-ThinkPad-T14s-Gen-1",
                  "cluster_name" : "datanode-cluster",
                  "cluster_uuid" : "-PmIXUWGQHukeW7275a9fg",
                  "version" : {
                    "distribution" : "opensearch",
                    "number" : "2.15.0",
                    "build_type" : "tar",
                    "build_hash" : "61dbcd0795c9bfe9b81e5762175414bc38bbcadf",
                    "build_date" : "2024-06-20T03:26:49.193630411Z",
                    "build_snapshot" : false,
                    "lucene_version" : "9.10.0",
                    "minimum_wire_compatibility_version" : "7.10.0",
                    "minimum_index_compatibility_version" : "7.0.0"
                  },
                  "tagline" : "The OpenSearch Project: https://opensearch.org/"
                }
                """;
    }
}

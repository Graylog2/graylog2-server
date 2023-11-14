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
package org.graylog.plugins.map.geoip;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.primitives.Longs;
import com.maxmind.db.MaxMindDbConstructor;
import com.maxmind.db.MaxMindDbParameter;

import javax.annotation.Nullable;

// IPInfo ASN response:
//
// {
//   "name" : "Cloudflare, Inc.",
//   "route" : "1.1.1.0/24",
//   "type" : "hosting",
//   "asn" : "AS13335",
//   "domain" : "cloudflare.com"
// }
public class IPinfoASN {
    private final String name;
    private final String route;
    private final String type;
    private final String asn;
    private final String domain;

    @MaxMindDbConstructor
    public IPinfoASN(@MaxMindDbParameter(name = "name") String name,
                     @MaxMindDbParameter(name = "route") String route,
                     @MaxMindDbParameter(name = "type") String type,
                     @MaxMindDbParameter(name = "asn") String asn,
                     @MaxMindDbParameter(name = "domain") String domain) {
        this.name = name;
        this.route = route;
        this.type = type;
        this.asn = asn;
        this.domain = domain;
    }

    @JsonProperty("name")
    @Nullable
    public String name() {
        return name;
    }

    @JsonProperty("route")
    @Nullable
    public String route() {
        return route;
    }

    @JsonProperty("type")
    @Nullable
    public String type() {
        return type;
    }

    @JsonProperty("asn")
    @Nullable
    public String asn() {
        return asn;
    }

    @JsonProperty("asn_numeric")
    @Nullable
    public Long asnNumeric() {
        final String asnValue = asn();
        if (asnValue == null) {
            return null;
        }
        return Longs.tryParse(asnValue.replace("AS", ""));
    }

    @JsonProperty("domain")
    @Nullable
    public String domain() {
        return domain;
    }
}

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
package org.graylog.security.shares;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog.security.Capability;

import java.util.Collection;

/**
 * The number of grantees per capability on one entity.
 */
public record ShareSummary(@JsonProperty("own") int own,
                           @JsonProperty("manage") int manage,
                           @JsonProperty("view") int view) {

    public static ShareSummary of(Collection<GranteeShare> shares) {
        int own = 0;
        int manage = 0;
        int view = 0;
        for (final GranteeShare share : shares) {
            switch (share.capability()) {
                case OWN -> own++;
                case MANAGE -> manage++;
                case VIEW -> view++;
            }
        }
        return new ShareSummary(own, manage, view);
    }

    public int total() {
        return own + manage + view;
    }
}

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
package org.graylog2.plugin;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public abstract class AbstractDescriptor {
    private final String name;
    private final boolean exclusive;
    private final String linkToDocs;

    // required for guice, but isn't called.
    protected AbstractDescriptor() {
        throw new IllegalStateException("This class " + this.getClass().getCanonicalName() + " should not be instantiated directly, this is a bug.");
    }

    protected AbstractDescriptor(String name, boolean exclusive, String linkToDocs) {
        this.name = name;
        this.exclusive = exclusive;
        this.linkToDocs = linkToDocs;
    }

    public String getName() {
        return name;
    }

    public boolean isExclusive() {
        return exclusive;
    }

    public String getLinkToDocs() {
        return linkToDocs;
    }
}

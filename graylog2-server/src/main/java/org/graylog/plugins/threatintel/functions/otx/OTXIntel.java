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
package org.graylog.plugins.threatintel.functions.otx;

import com.google.common.collect.Lists;

import java.util.List;

public class OTXIntel {

    private final List<OTXPulse> pulses;
    private final List<String> pulseIds;
    private final List<String> pulseNames;

    public OTXIntel() {
        this.pulses = Lists.newArrayList();
        this.pulseIds = Lists.newArrayList();
        this.pulseNames = Lists.newArrayList();
    }

    public void addPulse(OTXPulse pulse) {
        this.pulseIds.add(pulse.getId());
        this.pulseNames.add(pulse.getName());
        this.pulses.add(pulse);
    }

    public List<OTXPulse> getPulses() {
        return this.pulses;
    }

    public List<String> getPulseIds() {
        return pulseIds;
    }

    public List<String> getPulseNames() {
        return pulseNames;
    }

    public int getPulseCount() {
        return pulses.size();
    }

}

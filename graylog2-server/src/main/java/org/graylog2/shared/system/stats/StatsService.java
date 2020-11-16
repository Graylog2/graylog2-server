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
package org.graylog2.shared.system.stats;

import org.graylog2.shared.system.stats.fs.FsProbe;
import org.graylog2.shared.system.stats.fs.FsStats;
import org.graylog2.shared.system.stats.jvm.JvmProbe;
import org.graylog2.shared.system.stats.jvm.JvmStats;
import org.graylog2.shared.system.stats.network.NetworkProbe;
import org.graylog2.shared.system.stats.network.NetworkStats;
import org.graylog2.shared.system.stats.os.OsProbe;
import org.graylog2.shared.system.stats.os.OsStats;
import org.graylog2.shared.system.stats.process.ProcessProbe;
import org.graylog2.shared.system.stats.process.ProcessStats;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class StatsService {
    private final FsProbe fsProbe;
    private final JvmProbe jvmProbe;
    private final NetworkProbe networkProbe;
    private final OsProbe osProbe;
    private final ProcessProbe processProbe;

    @Inject
    public StatsService(FsProbe fsProbe,
                        JvmProbe jvmProbe,
                        NetworkProbe networkProbe,
                        OsProbe osProbe,
                        ProcessProbe processProbe) {
        this.fsProbe = fsProbe;
        this.jvmProbe = jvmProbe;
        this.networkProbe = networkProbe;
        this.osProbe = osProbe;
        this.processProbe = processProbe;
    }

    public FsStats fsStats() {
        return fsProbe.fsStats();
    }

    public JvmStats jvmStats() {
        return jvmProbe.jvmStats();
    }

    public NetworkStats networkStats() {
        return networkProbe.networkStats();
    }

    public OsStats osStats() {
        return osProbe.osStats();
    }

    public ProcessStats processStats() {
        return processProbe.processStats();
    }

    public SystemStats systemStats() {
        return SystemStats.create(fsStats(), jvmStats(), networkStats(), osStats(), processStats());
    }
}

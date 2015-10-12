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
package org.graylog2.shared.system.stats;

import com.google.inject.AbstractModule;
import org.graylog2.shared.system.stats.fs.FsProbe;
import org.graylog2.shared.system.stats.fs.JmxFsProbe;
import org.graylog2.shared.system.stats.fs.SigarFsProbe;
import org.graylog2.shared.system.stats.jvm.JvmProbe;
import org.graylog2.shared.system.stats.network.JmxNetworkProbe;
import org.graylog2.shared.system.stats.network.NetworkProbe;
import org.graylog2.shared.system.stats.network.SigarNetworkProbe;
import org.graylog2.shared.system.stats.os.JmxOsProbe;
import org.graylog2.shared.system.stats.os.OsProbe;
import org.graylog2.shared.system.stats.os.SigarOsProbe;
import org.graylog2.shared.system.stats.process.JmxProcessProbe;
import org.graylog2.shared.system.stats.process.ProcessProbe;
import org.graylog2.shared.system.stats.process.SigarProcessProbe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SystemStatsModule extends AbstractModule {
    private static final Logger LOG = LoggerFactory.getLogger(SystemStatsModule.class);
    private final boolean disableSigar;

    public SystemStatsModule(boolean disableSigar) {
        this.disableSigar = disableSigar;
    }

    @Override
    protected void configure() {
        boolean sigarLoaded = false;

        if(disableSigar) {
            LOG.debug("SIGAR disabled. Using JMX implementations.");
        } else {
            try {
                SigarService sigarService = new SigarService();
                if (sigarService.isReady()) {
                    bind(SigarService.class).toInstance(sigarService);
                    bind(FsProbe.class).to(SigarFsProbe.class).asEagerSingleton();
                    bind(NetworkProbe.class).to(SigarNetworkProbe.class).asEagerSingleton();
                    bind(OsProbe.class).to(SigarOsProbe.class).asEagerSingleton();
                    bind(ProcessProbe.class).to(SigarProcessProbe.class).asEagerSingleton();
                    sigarLoaded = true;
                }
            } catch (Throwable e) {
                LOG.debug("Failed to load SIGAR. Falling back to JMX implementations.", e);
            }
        }

        if (!sigarLoaded) {
            bind(FsProbe.class).to(JmxFsProbe.class).asEagerSingleton();
            bind(NetworkProbe.class).to(JmxNetworkProbe.class).asEagerSingleton();
            bind(OsProbe.class).to(JmxOsProbe.class).asEagerSingleton();
            bind(ProcessProbe.class).to(JmxProcessProbe.class).asEagerSingleton();
        }

        bind(JvmProbe.class).asEagerSingleton();
    }
}
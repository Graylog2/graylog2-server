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
package org.graylog2.system.stats;

import com.google.inject.AbstractModule;
import org.graylog2.shared.system.stats.SigarService;
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
import org.graylog2.system.stats.elasticsearch.ElasticsearchProbe;
import org.graylog2.system.stats.mongo.MongoProbe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusterStatsModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(ElasticsearchProbe.class).asEagerSingleton();
        bind(MongoProbe.class).asEagerSingleton();

        bind(ClusterStatsService.class).asEagerSingleton();
    }
}
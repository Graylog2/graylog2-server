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
package org.graylog2.bindings;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import org.graylog.scheduler.periodicals.ScheduleTriggerCleanUp;
import org.graylog2.events.ClusterEventCleanupPeriodical;
import org.graylog2.events.ClusterEventPeriodical;
import org.graylog2.indexer.fieldtypes.IndexFieldTypePollerPeriodical;
import org.graylog2.periodical.AlertScannerThread;
import org.graylog2.periodical.BatchedElasticSearchOutputFlushThread;
import org.graylog2.periodical.ClusterHealthCheckThread;
import org.graylog2.periodical.ClusterIdGeneratorPeriodical;
import org.graylog2.periodical.ConfigurationManagementPeriodical;
import org.graylog2.periodical.ContentPackLoaderPeriodical;
import org.graylog2.periodical.GarbageCollectionWarningThread;
import org.graylog2.periodical.IndexFailuresPeriodical;
import org.graylog2.periodical.IndexRangesCleanupPeriodical;
import org.graylog2.periodical.IndexRangesMigrationPeriodical;
import org.graylog2.periodical.IndexRetentionThread;
import org.graylog2.periodical.IndexRotationThread;
import org.graylog2.periodical.IndexerClusterCheckerThread;
import org.graylog2.periodical.LdapGroupMappingMigration;
import org.graylog2.periodical.NodePingThread;
import org.graylog2.periodical.ThrottleStateUpdaterThread;
import org.graylog2.periodical.TrafficCounterCalculator;
import org.graylog2.periodical.UserPermissionMigrationPeriodical;
import org.graylog2.periodical.VersionCheckThread;
import org.graylog2.plugin.periodical.Periodical;

public class PeriodicalBindings extends AbstractModule {
    @Override
    protected void configure() {
        Multibinder<Periodical> periodicalBinder = Multibinder.newSetBinder(binder(), Periodical.class);
        periodicalBinder.addBinding().to(AlertScannerThread.class);
        periodicalBinder.addBinding().to(BatchedElasticSearchOutputFlushThread.class);
        periodicalBinder.addBinding().to(ClusterHealthCheckThread.class);
        periodicalBinder.addBinding().to(ContentPackLoaderPeriodical.class);
        periodicalBinder.addBinding().to(GarbageCollectionWarningThread.class);
        periodicalBinder.addBinding().to(IndexerClusterCheckerThread.class);
        periodicalBinder.addBinding().to(IndexRetentionThread.class);
        periodicalBinder.addBinding().to(IndexRotationThread.class);
        periodicalBinder.addBinding().to(NodePingThread.class);
        periodicalBinder.addBinding().to(VersionCheckThread.class);
        periodicalBinder.addBinding().to(ThrottleStateUpdaterThread.class);
        periodicalBinder.addBinding().to(ClusterEventPeriodical.class);
        periodicalBinder.addBinding().to(ClusterEventCleanupPeriodical.class);
        periodicalBinder.addBinding().to(ClusterIdGeneratorPeriodical.class);
        periodicalBinder.addBinding().to(IndexRangesMigrationPeriodical.class);
        periodicalBinder.addBinding().to(IndexRangesCleanupPeriodical.class);
        periodicalBinder.addBinding().to(UserPermissionMigrationPeriodical.class);
        periodicalBinder.addBinding().to(ConfigurationManagementPeriodical.class);
        periodicalBinder.addBinding().to(LdapGroupMappingMigration.class);
        periodicalBinder.addBinding().to(IndexFailuresPeriodical.class);
        periodicalBinder.addBinding().to(TrafficCounterCalculator.class);
        periodicalBinder.addBinding().to(IndexFieldTypePollerPeriodical.class);
        periodicalBinder.addBinding().to(ScheduleTriggerCleanUp.class);
    }
}

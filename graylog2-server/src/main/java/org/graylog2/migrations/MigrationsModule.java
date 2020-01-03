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
package org.graylog2.migrations;

import org.graylog2.plugin.PluginModule;

public class MigrationsModule extends PluginModule {
    @Override
    protected void configure() {
        addMigration(V20151210140600_ElasticsearchConfigMigration.class);
        addMigration(V20161116172100_DefaultIndexSetMigration.class);
        addMigration(V20161116172200_CreateDefaultStreamMigration.class);
        addMigration(V20161122174500_AssignIndexSetsToStreamsMigration.class);
        addMigration(V20161124104700_AddRetentionRotationAndDefaultFlagToIndexSetMigration.class);
        addMigration(V20161125142400_EmailAlarmCallbackMigration.class);
        addMigration(V20161125161400_AlertReceiversMigration.class);
        addMigration(V20161130141500_DefaultStreamRecalcIndexRanges.class);
        addMigration(V20161215163900_MoveIndexSetDefaultConfig.class);
        addMigration(V20161216123500_DefaultIndexSetMigration.class);
        addMigration(V20170110150100_FixAlertConditionsMigration.class);
        addMigration(V20170607164210_MigrateReopenedIndicesToAliases.class);
        addMigration(V20180214093600_AdjustDashboardPositionToNewResolution.class);
        addMigration(V2018070614390000_EnforceUniqueGrokPatterns.class);
        addMigration(V20180718155800_AddContentPackIdAndRev.class);
        addMigration(V20180924111644_AddDefaultGrokPatterns.class);
        addMigration(V20190705071400_AddEventIndexSetsMigration.class);
        addMigration(V20190730100900_AddAlertsManagerRole.class);
        addMigration(V20190730000000_CreateDefaultEventsConfiguration.class);
        addMigration(V20191121145100_FixDefaultGrokPatterns.class);
        addMigration(V20200102140000_UnifyEventSeriesId.class);
    }
}

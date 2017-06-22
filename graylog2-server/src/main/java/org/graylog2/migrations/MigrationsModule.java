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

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

public class MigrationsModule extends AbstractModule {
    @Override
    protected void configure() {
        final Multibinder<Migration> binder = Multibinder.newSetBinder(binder(), Migration.class);
        binder.addBinding().to(V20151210140600_ElasticsearchConfigMigration.class);
        binder.addBinding().to(V20161116172100_DefaultIndexSetMigration.class);
        binder.addBinding().to(V20161116172200_CreateDefaultStreamMigration.class);
        binder.addBinding().to(V20161122174500_AssignIndexSetsToStreamsMigration.class);
        binder.addBinding().to(V20161124104700_AddRetentionRotationAndDefaultFlagToIndexSetMigration.class);
        binder.addBinding().to(V20161125142400_EmailAlarmCallbackMigration.class);
        binder.addBinding().to(V20161125161400_AlertReceiversMigration.class);
        binder.addBinding().to(V20161130141500_DefaultStreamRecalcIndexRanges.class);
        binder.addBinding().to(V20161215163900_MoveIndexSetDefaultConfig.class);
        binder.addBinding().to(V20161216123500_DefaultIndexSetMigration.class);
        binder.addBinding().to(V20170110150100_FixAlertConditionsMigration.class);
        binder.addBinding().to(V20170607164210_MigrateReopenedIndicesToAliases.class);
    }
}

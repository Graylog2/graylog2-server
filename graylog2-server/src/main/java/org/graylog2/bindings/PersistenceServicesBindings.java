/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.bindings;

import com.google.inject.AbstractModule;
import org.graylog2.alarmcallbacks.AlarmCallbackConfigurationService;
import org.graylog2.alarmcallbacks.AlarmCallbackConfigurationServiceImpl;
import org.graylog2.alerts.AlertService;
import org.graylog2.alerts.AlertServiceImpl;
import org.graylog2.cluster.NodeService;
import org.graylog2.cluster.NodeServiceImpl;
import org.graylog2.dashboards.DashboardService;
import org.graylog2.dashboards.DashboardServiceImpl;
import org.graylog2.indexer.IndexFailureService;
import org.graylog2.indexer.IndexFailureServiceImpl;
import org.graylog2.indexer.PersistedDeadLetterService;
import org.graylog2.indexer.PersistedDeadLetterServiceImpl;
import org.graylog2.indexer.ranges.IndexRangeService;
import org.graylog2.indexer.ranges.IndexRangeServiceImpl;
import org.graylog2.inputs.InputService;
import org.graylog2.inputs.InputServiceImpl;
import org.graylog2.notifications.NotificationService;
import org.graylog2.notifications.NotificationServiceImpl;
import org.graylog2.savedsearches.SavedSearchService;
import org.graylog2.savedsearches.SavedSearchServiceImpl;
import org.graylog2.security.AccessTokenService;
import org.graylog2.security.AccessTokenServiceImpl;
import org.graylog2.security.MongoDBSessionService;
import org.graylog2.security.MongoDBSessionServiceImpl;
import org.graylog2.security.ldap.LdapSettingsService;
import org.graylog2.security.ldap.LdapSettingsServiceImpl;
import org.graylog2.streams.*;
import org.graylog2.system.activities.SystemMessageService;
import org.graylog2.system.activities.SystemMessageServiceImpl;
import org.graylog2.users.UserService;
import org.graylog2.users.UserServiceImpl;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class PersistenceServicesBindings extends AbstractModule {
    @Override
    protected void configure() {
        bind(SystemMessageService.class).to(SystemMessageServiceImpl.class);
        bind(DashboardService.class).to(DashboardServiceImpl.class);
        bind(AlertService.class).to(AlertServiceImpl.class);
        bind(NotificationService.class).to(NotificationServiceImpl.class);
        bind(PersistedDeadLetterService.class).to(PersistedDeadLetterServiceImpl.class);
        bind(IndexFailureService.class).to(IndexFailureServiceImpl.class);
        bind(NodeService.class).to(NodeServiceImpl.class);
        bind(IndexRangeService.class).to(IndexRangeServiceImpl.class);
        bind(InputService.class).to(InputServiceImpl.class);
        bind(StreamRuleService.class).to(StreamRuleServiceImpl.class);
        bind(UserService.class).to(UserServiceImpl.class);
        bind(StreamService.class).to(StreamServiceImpl.class);
        bind(AccessTokenService.class).to(AccessTokenServiceImpl.class);
        bind(SavedSearchService.class).to(SavedSearchServiceImpl.class);
        bind(LdapSettingsService.class).to(LdapSettingsServiceImpl.class);
        bind(MongoDBSessionService.class).to(MongoDBSessionServiceImpl.class);
        bind(AlarmCallbackConfigurationService.class).to(AlarmCallbackConfigurationServiceImpl.class);
    }
}

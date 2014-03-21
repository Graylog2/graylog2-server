/*
 * Copyright 2012-2014 TORCH GmbH
 *
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
import org.graylog2.notifications.NotificationService;
import org.graylog2.notifications.NotificationServiceImpl;
import org.graylog2.system.activities.SystemMessageService;
import org.graylog2.system.activities.SystemMessageServiceImpl;

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
    }
}

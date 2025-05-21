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

import type { PluginExports } from 'graylog-web-plugin/plugin';

import Routes from 'routing/Routes';

const ALERTS_TITLE = 'Alerts & Events';
const EVENT_PROCEDURES_TITLE = 'Event Procedures';
const EVENT_DEFINITIONS_TITLE = 'Event Definitions';
const NOTIFICATIONS_TITLE = 'Notifications';

const eventsBindings: PluginExports = {
  'alerts.pageNavigation': [
    { description: ALERTS_TITLE, path: Routes.ALERTS.LIST },
    { description: EVENT_PROCEDURES_TITLE, path: Routes.ALERTS.EVENT_PROCEDURES.LIST('procedures') },
    { description: EVENT_DEFINITIONS_TITLE, path: Routes.ALERTS.DEFINITIONS.LIST },
    { description: NOTIFICATIONS_TITLE, path: Routes.ALERTS.NOTIFICATIONS.LIST },
  ],
};

export default eventsBindings;

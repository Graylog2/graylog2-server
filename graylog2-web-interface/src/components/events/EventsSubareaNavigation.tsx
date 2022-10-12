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
import * as React from 'react';

import SubareaNavigation from 'components/common/SubareaNavigation';
import Routes from 'routing/Routes';

const NAV_ITEMS = [
  { title: 'Alerts & Events', path: Routes.ALERTS.LIST },
  { title: 'Event Definitions', path: Routes.ALERTS.DEFINITIONS.LIST, permissions: 'eventdefinitions:read' },
  { title: 'Notifications', path: Routes.ALERTS.NOTIFICATIONS.LIST, permissions: 'eventnotifications:read' },
];

const EventsSubareaNavigation = () => {
  return (<SubareaNavigation items={NAV_ITEMS} />);
};

export default EventsSubareaNavigation;

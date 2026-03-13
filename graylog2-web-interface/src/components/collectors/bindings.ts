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
import {TELEMETRY_EVENT_TYPE} from 'logic/telemetry/Constants';

export const PAGE_NAV_TITLE = 'Collectors';

const bindings: PluginExports = {
  pageNavigation: [
    {
      description: PAGE_NAV_TITLE,
      children: [
        { description: 'Overview', path: Routes.SYSTEM.COLLECTORS.OVERVIEW, exactPathMatch: true },
        { description: 'Fleets', path: Routes.SYSTEM.COLLECTORS.FLEETS },
        { description: 'Instances', path: Routes.SYSTEM.COLLECTORS.INSTANCES },
        { description: 'Deployment', path: Routes.SYSTEM.COLLECTORS.DEPLOYMENT },
        { description: 'Settings', path: Routes.SYSTEM.COLLECTORS.SETTINGS },
      ],
    },
  ],
  entityCreators: [
    {
      id: 'Fleet',
      title: 'Create fleet',
      path: Routes.SYSTEM.COLLECTORS.FLEETS_NEW,
      telemetryEvent: {
        type: TELEMETRY_EVENT_TYPE.COLLECTORS.FLEET_NEW_OPENED,
        section: 'collectors',
        actionValue: 'create-fleet-button',
      },
// TODO we don't have permissions yet      permissions: 'fleets:create',
    },
  ],
};

export default bindings;

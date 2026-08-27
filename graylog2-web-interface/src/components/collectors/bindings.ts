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
import type { Permission, PluginExports } from 'graylog-web-plugin/plugin';

import Routes from 'routing/Routes';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import AppConfig from 'util/AppConfig';
import { COLLECTOR_PERMISSIONS } from 'components/collectors/Permissions';

export const PAGE_NAV_TITLE = 'Collectors';

const bindings: PluginExports = AppConfig.isFeatureEnabled('collectors')
  ? {
      pageNavigation: [
        {
          description: PAGE_NAV_TITLE,
          children: [
            { description: 'Overview', path: Routes.SYSTEM.COLLECTORS.OVERVIEW, exactPathMatch: true },
            { description: 'Fleets', path: Routes.SYSTEM.COLLECTORS.FLEETS },
            { description: 'Instances', path: Routes.SYSTEM.COLLECTORS.INSTANCES },
            { description: 'Deployment', path: Routes.SYSTEM.COLLECTORS.DEPLOYMENT },
            {
              description: 'Settings',
              path: Routes.SYSTEM.COLLECTORS.SETTINGS,
              permissions: [COLLECTOR_PERMISSIONS.CONFIGURATION_READ],
            },
          ],
        },
      ],
      entityCreators: [
        {
          id: 'Fleet',
          title: 'Create fleet',
          path: Routes.SYSTEM.COLLECTORS.FLEETS_NEW,
          telemetryEvent: {
            type: TELEMETRY_EVENT_TYPE.COLLECTORS.FLEET.CREATE_OPENED,
            section: 'collectors',
            actionValue: 'create-fleet-button',
          },
          permissions: COLLECTOR_PERMISSIONS.FLEET_CREATE as Permission,
        },
      ],
    }
  : {};

export default bindings;

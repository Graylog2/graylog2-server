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
};

export default bindings;

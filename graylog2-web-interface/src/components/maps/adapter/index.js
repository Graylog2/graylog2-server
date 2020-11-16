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
import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import MaxmindAdapterFieldSet from 'components/maps/adapter/MaxmindAdapterFieldSet';
import MaxmindAdapterSummary from 'components/maps/adapter/MaxmindAdapterSummary';
import MaxmindAdapterDocumentation from 'components/maps/adapter/MaxmindAdapterDocumentation';

PluginStore.register(new PluginManifest({}, {
  lookupTableAdapters: [
    {
      type: 'maxmind_geoip',
      displayName: 'Geo IP - MaxMind\u2122 Databases',
      formComponent: MaxmindAdapterFieldSet,
      summaryComponent: MaxmindAdapterSummary,
      documentationComponent: MaxmindAdapterDocumentation,
    },
  ],
}));

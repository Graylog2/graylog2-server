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

import NullCacheFieldSet from './NullCacheFieldSet';
import NullCacheSummary from './NullCacheSummary';
import CaffeineCacheFieldSet from './CaffeineCacheFieldSet';
import CaffeineCacheSummary from './CaffeineCacheSummary';
import CaffeineCacheDocumentation from './CaffeineCacheDocumentation';

PluginStore.register(new PluginManifest({}, {
  lookupTableCaches: [
    {
      type: 'none',
      displayName: 'Do not cache values',
      formComponent: NullCacheFieldSet,
      summaryComponent: NullCacheSummary,
      documentationComponent: null,
    },
    {
      type: 'guava_cache', // old name kept for backwards compatibility
      displayName: 'Node-local, in-memory cache',
      formComponent: CaffeineCacheFieldSet,
      summaryComponent: CaffeineCacheSummary,
      documentationComponent: CaffeineCacheDocumentation,
    },
  ],
}));

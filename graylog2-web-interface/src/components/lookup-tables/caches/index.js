import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import NullCacheFieldSet from './NullCacheFieldSet';
import NullCacheSummary from './NullCacheSummary';
import GuavaCacheFieldSet from './GuavaCacheFieldSet';
import GuavaCacheSummary from './GuavaCacheSummary';

PluginStore.register(new PluginManifest({}, {
  lookupTableCaches: [
    {
      type: 'none',
      displayName: 'Do not cache values',
      formComponent: NullCacheFieldSet,
      summaryComponent: NullCacheSummary,
    },
    {
      type: 'guava_cache',
      displayName: 'Node-local, in-memory cache',
      formComponent: GuavaCacheFieldSet,
      summaryComponent: GuavaCacheSummary,
    },
  ],
}));

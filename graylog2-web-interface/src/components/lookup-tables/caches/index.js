import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import NullCacheFieldSet from './NullCacheFieldSet';
import NullCacheSummary from './NullCacheSummary';
import GuavaCaffeineCacheFieldSet from './GuavaCaffeineCacheFieldSet';
import GuavaCaffeineCacheSummary from './GuavaCaffeineCacheSummary';
import GuavaCaffeineCacheDocumentation from './GuavaCaffeineCacheDocumentation';

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
      type: 'caffeine_cache',
      displayName: 'Node-local, in-memory, Caffeine based cache',
      formComponent: GuavaCaffeineCacheFieldSet,
      summaryComponent: GuavaCaffeineCacheSummary,
      documentationComponent: GuavaCaffeineCacheDocumentation,
    },
    {
      type: 'guava_cache',
      displayName: 'Node-local, in-memory, Guava based cache',
      formComponent: GuavaCaffeineCacheFieldSet,
      summaryComponent: GuavaCaffeineCacheSummary,
      documentationComponent: GuavaCaffeineCacheDocumentation,
    },
  ],
}));

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

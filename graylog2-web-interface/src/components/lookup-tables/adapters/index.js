import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import RandomAdapterFieldSet from './RandomAdapterFieldSet';
import RandomAdapterSummary from './RandomAdapterSummary';

PluginStore.register(new PluginManifest({}, {
  lookupTableAdapters: [
    {
      type: 'random',
      displayName: 'Random 32bit integer source',
      formComponent: RandomAdapterFieldSet,
      summaryComponent: RandomAdapterSummary,
    },
  ],
}));

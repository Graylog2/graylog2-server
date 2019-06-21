import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import FilterAggregationForm from './FilterAggregationForm';

PluginStore.register(new PluginManifest({}, {
  eventDefinitionTypes: [
    {
      type: 'aggregation-v1',
      displayName: 'Filter & Aggregation',
      formComponent: FilterAggregationForm,
    },
  ],
}));

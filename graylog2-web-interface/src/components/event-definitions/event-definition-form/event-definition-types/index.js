import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import FilterAggregationFormContainer from './FilterAggregationFormContainer';

PluginStore.register(new PluginManifest({}, {
  eventDefinitionTypes: [
    {
      type: 'aggregation-v1',
      displayName: 'Filter & Aggregation',
      formComponent: FilterAggregationFormContainer,
    },
  ],
}));

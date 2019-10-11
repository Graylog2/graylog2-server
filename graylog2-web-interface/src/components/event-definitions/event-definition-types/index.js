import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import FilterAggregationFormContainer from './FilterAggregationFormContainer';
import FilterAggregationForm from './FilterAggregationForm';
import FilterAggregationSummary from './FilterAggregationSummary';

PluginStore.register(new PluginManifest({}, {
  eventDefinitionTypes: [
    {
      type: 'aggregation-v1',
      displayName: 'Filter & Aggregation',
      sortOrder: 0, // Sort before conditions working on events
      description: 'Create Events from log messages by filtering them and (optionally) '
        + 'aggregating their results to match a given condition. These Events can be used as input for a Correlation Rule.',
      formComponent: FilterAggregationFormContainer,
      summaryComponent: FilterAggregationSummary,
      defaultConfig: FilterAggregationForm.defaultConfig,
    },
  ],
}));

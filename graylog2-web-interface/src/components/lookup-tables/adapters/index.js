import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import RandomAdapterFieldSet from './RandomAdapterFieldSet';
import RandomAdapterSummary from './RandomAdapterSummary';
import CSVFileAdapterFieldSet from './CSVFileAdapterFieldSet';
import CSVFileAdapterSummary from './CSVFileAdapterSummary';
import CSVFileAdapterDocumentation from './CSVFileAdapterDocumentation';

PluginStore.register(new PluginManifest({}, {
  lookupTableAdapters: [
    {
      type: 'random',
      displayName: 'Random 32bit integer source',
      formComponent: RandomAdapterFieldSet,
      summaryComponent: RandomAdapterSummary,
      documentationComponent: null,
    },
    {
      type: 'csvfile',
      displayName: 'CSV File',
      formComponent: CSVFileAdapterFieldSet,
      summaryComponent: CSVFileAdapterSummary,
      documentationComponent: CSVFileAdapterDocumentation,
    },
  ],
}));

import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import CSVFileAdapterFieldSet from './CSVFileAdapterFieldSet';
import CSVFileAdapterSummary from './CSVFileAdapterSummary';
import CSVFileAdapterDocumentation from './CSVFileAdapterDocumentation';
import HTTPJSONPathAdapterFieldSet from './HTTPJSONPathAdapterFieldSet';
import HTTPJSONPathAdapterSummary from './HTTPJSONPathAdapterSummary';
import HTTPJSONPathAdapterDocumentation from './HTTPJSONPathAdapterDocumentation';

PluginStore.register(new PluginManifest({}, {
  lookupTableAdapters: [
    {
      type: 'csvfile',
      displayName: 'CSV File',
      formComponent: CSVFileAdapterFieldSet,
      summaryComponent: CSVFileAdapterSummary,
      documentationComponent: CSVFileAdapterDocumentation,
    },
    {
      type: 'httpjsonpath',
      displayName: 'HTTP JSONPath',
      formComponent: HTTPJSONPathAdapterFieldSet,
      summaryComponent: HTTPJSONPathAdapterSummary,
      documentationComponent: HTTPJSONPathAdapterDocumentation,
    },
  ],
}));

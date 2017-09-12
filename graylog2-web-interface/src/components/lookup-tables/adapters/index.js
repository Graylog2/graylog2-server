import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import CSVFileAdapterFieldSet from './CSVFileAdapterFieldSet';
import CSVFileAdapterSummary from './CSVFileAdapterSummary';
import CSVFileAdapterDocumentation from './CSVFileAdapterDocumentation';
import DSVHTTPAdapterFieldSet from './CSVFileAdapterFieldSet';
import DSVHTTPAdapterSummary from './DSVHTTPAdapterSummary';
import DSVHTTPAdapterDocumentation from './DSVHTTPAdapterDocumentation';
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
    {
      type: 'dsvhttp',
      displayName: 'DSV File from HTTP',
      formComponent: DSVHTTPAdapterFieldSet,
      summaryComponent: DSVHTTPAdapterSummary,
      documentationComponent: DSVHTTPAdapterDocumentation,
    },
  ],
}));

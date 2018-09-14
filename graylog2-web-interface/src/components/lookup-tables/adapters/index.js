import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import CSVFileAdapterFieldSet from './CSVFileAdapterFieldSet';
import CSVFileAdapterSummary from './CSVFileAdapterSummary';
import CSVFileAdapterDocumentation from './CSVFileAdapterDocumentation';
import DnsAdapterFieldSet from './DnsAdapterFieldSet';
import DnsAdapterSummary from './DnsAdapterSummary';
import DnsAdapterDocumentation from './DnsAdapterDocumentation';
import DSVHTTPAdapterFieldSet from './DSVHTTPAdapterFieldSet';
import DSVHTTPAdapterSummary from './DSVHTTPAdapterSummary';
import DSVHTTPAdapterDocumentation from './DSVHTTPAdapterDocumentation';
import HTTPJSONPathAdapterFieldSet from './HTTPJSONPathAdapterFieldSet';
import HTTPJSONPathAdapterSummary from './HTTPJSONPathAdapterSummary';
import HTTPJSONPathAdapterDocumentation from './HTTPJSONPathAdapterDocumentation';
import {} from 'components/maps/adapter';

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
      type: 'dnslookup',
      displayName: 'DNS Lookup',
      formComponent: DnsAdapterFieldSet,
      summaryComponent: DnsAdapterSummary,
      documentationComponent: DnsAdapterDocumentation,
    },
    {
      type: 'dsvhttp',
      displayName: 'DSV File from HTTP',
      formComponent: DSVHTTPAdapterFieldSet,
      summaryComponent: DSVHTTPAdapterSummary,
      documentationComponent: DSVHTTPAdapterDocumentation,
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

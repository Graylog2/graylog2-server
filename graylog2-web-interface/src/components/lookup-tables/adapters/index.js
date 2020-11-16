/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import {} from 'components/maps/adapter';
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

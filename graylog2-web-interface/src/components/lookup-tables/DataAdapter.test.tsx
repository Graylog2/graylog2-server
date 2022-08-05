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
import * as React from 'react';
import { render, screen } from 'wrappedTestingLibrary';
import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import CSVFileAdapterSummary from './adapters/CSVFileAdapterSummary';
import { DATA_ADAPTER, mockedUseScopePermissions } from './fixtures';
import DataAdapter from './DataAdapter';

jest.mock('hooks/useScopePermissions', () => (mockedUseScopePermissions));

PluginStore.register(new PluginManifest({}, {
  lookupTableAdapters: [
    {
      type: 'csvfile',
      displayName: 'CSV File',
      summaryComponent: CSVFileAdapterSummary,
    },
  ],
}));

const renderedDataAdapter = (scope: string) => {
  const auxDataAdapter = { ...DATA_ADAPTER };
  auxDataAdapter._scope = scope;

  auxDataAdapter.config = {
    type: 'csvfile',
    path: '/data/node-01/illuminate/csv/ciscoasa/data/cisco_asa_event_codes.csv',
    override_type: 'mongo',
    separator: ',',
    quotechar: '"',
    key_column: 'cisco_event_code',
    value_column: 'gim_event_type_code',
    check_interval: 60,
    case_insensitive_lookup: false,
  };

  return render(
    <DataAdapter dataAdapter={auxDataAdapter} />,
  );
};

describe('DataAdapter', () => {
  it('should show "edit" button', async () => {
    renderedDataAdapter('DEFAULT');

    expect(screen.getByRole('button', { name: /edit/i })).toBeInTheDocument();
  });

  it('should not show "edit" button', async () => {
    renderedDataAdapter('ILLUMINATE');

    expect(screen.queryByRole('button', { name: /edit/i })).not.toBeInTheDocument();
  });
});

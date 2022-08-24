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

import { createLookupTableAdapter } from 'fixtures/lookupTables';
import { asMock } from 'helpers/mocking';
import useScopePermissions from 'hooks/useScopePermissions';
import type { GenericEntityType } from 'logic/lookup-tables/types';

import CSVFileAdapterSummary from './adapters/CSVFileAdapterSummary';
import DataAdapter from './DataAdapter';

jest.mock('hooks/useScopePermissions');

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
  const dataAdapter = createLookupTableAdapter(1, { _scope: scope });

  return render(<DataAdapter dataAdapter={dataAdapter} />);
};

describe('DataAdapter', () => {
  beforeAll(() => {
    asMock(useScopePermissions).mockImplementation(
      (entity: GenericEntityType) => {
        const scopes = {
          ILLUMINATE: { is_mutable: false },
          DEFAULT: { is_mutable: true },
        };

        return {
          loadingScopePermissions: false,
          scopePermissions: scopes[entity._scope],
        };
      },
    );
  });

  it('should show "edit" button', async () => {
    renderedDataAdapter('DEFAULT');

    expect(screen.getByRole('button', { name: /edit/i })).toBeInTheDocument();
  });

  it('should not show "edit" button', async () => {
    renderedDataAdapter('ILLUMINATE');

    expect(screen.queryByRole('button', { name: /edit/i })).not.toBeInTheDocument();
  });
});

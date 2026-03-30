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
import userEvent from '@testing-library/user-event';
import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import { createLookupTableAdapter } from 'fixtures/lookupTables';
import { asMock } from 'helpers/mocking';
import useScopePermissions from 'hooks/useScopePermissions';
import type { GenericEntityType } from 'logic/lookup-tables/types';
import { ModalProvider } from 'components/lookup-tables/contexts/ModalContext';
import { lookupDataAdapter } from 'components/lookup-tables/hooks/api/lookupTablesAPI';

import CSVFileAdapterSummary from './adapters/CSVFileAdapterSummary';
import DataAdapter from './DataAdapter';

jest.mock('hooks/useScopePermissions');
jest.mock('components/lookup-tables/hooks/api/lookupTablesAPI', () => ({
  ...jest.requireActual('components/lookup-tables/hooks/api/lookupTablesAPI'),
  lookupDataAdapter: jest.fn(),
}));

PluginStore.register(
  new PluginManifest(
    {},
    {
      lookupTableAdapters: [
        {
          type: 'csvfile',
          displayName: 'CSV File',
          summaryComponent: CSVFileAdapterSummary,
        },
      ],
    },
  ),
);

const renderedDataAdapter = (scope: string) => {
  const dataAdapter = createLookupTableAdapter(1, { _scope: scope });

  return render(
    <ModalProvider>
      <DataAdapter dataAdapter={dataAdapter} />
    </ModalProvider>,
  );
};

describe('DataAdapter', () => {
  beforeAll(() => {
    asMock(useScopePermissions).mockImplementation((entity: GenericEntityType) => {
      const scopes = {
        ILLUMINATE: { is_mutable: false },
        DEFAULT: { is_mutable: true },
      };

      return {
        loadingScopePermissions: false,
        scopePermissions: scopes[entity?._scope || 'DEFAULT'],
        checkPermissions: (inEntity: Partial<GenericEntityType>) => {
          const entityScope = inEntity?._scope?.toUpperCase() || 'DEFAULT';

          return scopes[entityScope].is_mutable;
        },
      };
    });
  });

  it('should show "edit" button', async () => {
    renderedDataAdapter('DEFAULT');

    expect(screen.getByRole('button', { name: /edit/i })).toBeInTheDocument();
  });

  it('should not show "edit" button', async () => {
    renderedDataAdapter('ILLUMINATE');

    expect(screen.queryByRole('button', { name: /edit/i })).not.toBeInTheDocument();
  });

  // Regression: PR #23432 introduced json-with-bigint which deserializes large numbers as BigInt.
  // DataAdapter used native JSON.stringify to render lookup results, which throws
  // "TypeError: Do not know how to serialize a BigInt" for values like AD's accountExpires.
  // Introduced by: 5cb51acdcceb660c2cc75fc8c0ad48a17b543334 (2026-03-19)
  it('should render lookup results containing BigInt values without crashing', async () => {
    const lookupResultWithBigInt = {
      single_value: 'testuser',
      multi_value: {
        sAMAccountName: 'testuser',
        accountExpires: BigInt('9223372036854775807'),
        pwdLastSet: BigInt('134185088795495957'),
        displayName: 'Test User',
      },
      has_error: false,
      ttl: 1000,
    };

    asMock(lookupDataAdapter).mockResolvedValue(lookupResultWithBigInt);

    renderedDataAdapter('DEFAULT');

    const keyInput = screen.getByLabelText(/key/i);
    const lookupButton = screen.getByRole('button', { name: /look up/i });

    await userEvent.type(keyInput, 'testuser');
    await userEvent.click(lookupButton);

    await screen.findByText(/lookup result/i);

    expect(screen.getByText(/9223372036854775807/)).toBeInTheDocument();
    expect(screen.getByText(/testuser/)).toBeInTheDocument();
  });
});

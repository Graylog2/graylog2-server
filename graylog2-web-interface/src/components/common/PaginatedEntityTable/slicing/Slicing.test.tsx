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
import { screen, render, waitFor, within } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import type { SearchParams } from 'stores/PaginationTypes';
import TableFetchContext, { type ContextValue } from 'components/common/PaginatedEntityTable/TableFetchContext';

import Slicing from './index';

jest.mock('logic/telemetry/useSendTelemetry', () => () => jest.fn());

describe('Slicing', () => {
  const columnSchemas = [
    { id: 'title', title: 'Title', type: 'STRING' as const, sliceable: true },
    { id: 'status', title: 'Status', type: 'STRING' as const, sliceable: true },
    { id: 'description', title: 'Description', type: 'STRING' as const, sliceable: false },
  ];

  const renderSUT = (
    props: Partial<React.ComponentProps<typeof Slicing>> = {},
    contextOverrides: Partial<ContextValue> & { searchParams?: Partial<SearchParams> } = {},
  ) => {
    const searchParams = {
      page: 1,
      pageSize: 10,
      query: '',
      sort: { attributeId: 'title', direction: 'asc' },
      sliceCol: 'status',
      slice: undefined,
      filters: undefined,
      ...contextOverrides.searchParams,
    };
    const contextValue: ContextValue = {
      searchParams,
      refetch: jest.fn(),
      attributes: [],
      entityTableId: 'test-entity-table',
      ...contextOverrides,
    };

    return render(
      <TableFetchContext.Provider value={contextValue}>
        <Slicing
          appSection="test-app-section"
          columnSchemas={columnSchemas}
          onChangeSlicing={() => {}}
          fetchSlices={() => Promise.resolve({ slices: [] })}
          {...props}
        />
      </TableFetchContext.Provider>,
    );
  };

  it('displays slice options', async () => {
    renderSUT();

    const button = await screen.findByRole('button', { name: /status/i });
    await userEvent.click(button);

    await screen.findByRole('menuitem', { name: /title/i });
    await screen.findByRole('menuitem', { name: /status/i });
    expect(screen.queryByRole('menuitem', { name: /description/i })).not.toBeInTheDocument();
  });

  it('selects a slice', async () => {
    const onChangeSlicing = jest.fn();
    renderSUT({ onChangeSlicing });

    const button = await screen.findByRole('button', { name: /status/i });
    await userEvent.click(button);

    const menuItem = await screen.findByRole('menuitem', { name: /title/i });
    await userEvent.click(menuItem);

    expect(onChangeSlicing).toHaveBeenCalledWith('title');
  });

  it('removes slicing', async () => {
    const onChangeSlicing = jest.fn();
    renderSUT({ onChangeSlicing });

    const button = await screen.findByRole('button', { name: /status/i });
    await userEvent.click(button);

    const menuItem = await screen.findByRole('menuitem', { name: /no slicing/i });
    await userEvent.click(menuItem);

    expect(onChangeSlicing).toHaveBeenCalledWith(undefined, undefined);
  });

  it('filters slices based on search query', async () => {
    renderSUT({
      fetchSlices: () =>
        Promise.resolve({
          slices: [
            { value: 'Alpha', count: 2 },
            { value: 'Beta', count: 1 },
          ],
        }),
    });

    await screen.findByText('Alpha');

    await userEvent.type(screen.getByPlaceholderText(/filter status/i), 'alp');

    expect(screen.getByText('Alpha')).toBeInTheDocument();
    expect(screen.queryByText('Beta')).not.toBeInTheDocument();
  });

  it('sorts slices by count', async () => {
    renderSUT({
      fetchSlices: () =>
        Promise.resolve({
          slices: [
            { value: 'Alpha', count: 1 },
            { value: 'Beta', count: 3 },
          ],
        }),
    });

    await screen.findByText('Alpha');

    const getItems = () => within(screen.getByTestId('slices-list')).getAllByRole('button');

    expect(getItems()[0]).toHaveTextContent('Alpha');
    expect(getItems()[1]).toHaveTextContent('Beta');

    await userEvent.click(screen.getByRole('button', { name: /a-z/i }));
    await userEvent.click(await screen.findByRole('menuitem', { name: /count/i }));

    await waitFor(() => {
      expect(getItems()[0]).toHaveTextContent('Beta');
      expect(getItems()[1]).toHaveTextContent('Alpha');
    });
  });

  it('shows empty slices when toggled', async () => {
    renderSUT({
      fetchSlices: () =>
        Promise.resolve({
          slices: [
            { value: 'Alpha', count: 1 },
            { value: 'Gamma', count: 0 },
          ],
        }),
    });

    await screen.findByText('Alpha');

    expect(screen.queryByText('Gamma')).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: /show empty slices/i }));

    expect(await screen.findByText('Gamma')).toBeInTheDocument();
  });
});

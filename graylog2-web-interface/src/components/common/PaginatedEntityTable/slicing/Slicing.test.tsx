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
import { defaultUser } from 'defaultMockValues';

import { asMock } from 'helpers/mocking';
import type { SearchParams } from 'stores/PaginationTypes';
import TableFetchContext, { type ContextValue } from 'components/common/PaginatedEntityTable/TableFetchContext';
import type { ColumnSchema } from 'components/common/EntityDataTable/types';
import useCurrentUser from 'hooks/useCurrentUser';

import Slicing from './index';

jest.mock('logic/telemetry/useSendTelemetry', () => () => jest.fn());
jest.mock('hooks/useCurrentUser');

describe('Slicing', () => {
  const columnSchemas: Array<ColumnSchema> = [
    { id: 'title', title: 'Title', type: 'STRING' as const, sliceable: true },
    {
      id: 'status',
      title: 'Status',
      type: 'STRING' as const,
      sliceable: true,
      permissions: ['streams:read'],
      slice_sort_options: [{ value: 'risk_score', title: 'Risk Score' }],
    },
    { id: 'owner', title: 'Owner', type: 'STRING' as const, sliceable: true, permissions: ['roles:read'] },
    { id: 'description', title: 'Description', type: 'STRING' as const, sliceable: false },
  ];

  beforeEach(() => {
    asMock(useCurrentUser).mockReturnValue(defaultUser);
  });

  const renderSUT = (
    props: Partial<React.ComponentProps<typeof Slicing>> = {},
    contextOverrides: { searchParams?: Partial<SearchParams> } = {},
  ) => {
    const searchParams: SearchParams = {
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
    await screen.findByRole('menuitem', { name: /owner/i });
    await screen.findByRole('menuitem', { name: /status/i });
    expect(screen.queryByRole('menuitem', { name: /description/i })).not.toBeInTheDocument();
  });

  it('hides slice options for columns without the required permissions', async () => {
    asMock(useCurrentUser).mockReturnValue(defaultUser.toBuilder().permissions(['streams:read']).build());

    renderSUT();

    const button = await screen.findByRole('button', { name: /status/i });
    await userEvent.click(button);

    await screen.findByRole('menuitem', { name: /title/i });
    await screen.findByRole('menuitem', { name: /status/i });
    expect(screen.queryByRole('menuitem', { name: /owner/i })).not.toBeInTheDocument();
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

    const noSlicingButton = await screen.findByRole('button', { name: /no slicing/i });
    await userEvent.click(noSlicingButton);

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

    await userEvent.type(screen.getByPlaceholderText(/filter/i), 'alp');

    expect(screen.getByText('Alpha')).toBeInTheDocument();
    expect(screen.queryByText('Beta')).not.toBeInTheDocument();
  });

  it('does not display count as a slice sort option', async () => {
    renderSUT();

    await userEvent.click(await screen.findByRole('button', { name: /alphabetical/i }));

    expect(screen.queryByRole('menuitem', { name: /count/i })).not.toBeInTheDocument();
  });

  it('displays additional slice sort options from the active column schema', async () => {
    renderSUT();

    await userEvent.click(await screen.findByRole('button', { name: /alphabetical/i }));

    await screen.findByRole('menuitem', { name: /risk score/i });
  });

  it('sorts slices by additional slice sort metadata', async () => {
    renderSUT({
      fetchSlices: () =>
        Promise.resolve({
          slices: [
            { value: 'Alpha', count: 1, meta: { risk_score: 2 } },
            { value: 'Beta', count: 1, meta: { risk_score: 10 } },
          ],
        }),
    });

    await screen.findByText('Alpha');

    const getItems = () => within(screen.getByTestId('slices-list')).getAllByRole('button');

    await userEvent.click(screen.getByRole('button', { name: /alphabetical/i }));
    await userEvent.click(await screen.findByRole('menuitem', { name: /risk score/i }));

    await waitFor(() => {
      expect(getItems()[0]).toHaveTextContent('Beta');
      expect(getItems()[1]).toHaveTextContent('Alpha');
    });
  });

  it('toggles the slice sort direction', async () => {
    renderSUT({
      fetchSlices: () =>
        Promise.resolve({
          slices: [
            { value: 'Alpha', count: 1 },
            { value: 'Beta', count: 1 },
          ],
        }),
    });

    await screen.findByText('Alpha');

    const getItems = () => within(screen.getByTestId('slices-list')).getAllByRole('button');

    expect(getItems()[0]).toHaveTextContent('Alpha');
    expect(getItems()[1]).toHaveTextContent('Beta');

    await userEvent.click(screen.getByRole('button', { name: /sort ascending/i }));

    await waitFor(() => {
      expect(getItems()[0]).toHaveTextContent('Beta');
      expect(getItems()[1]).toHaveTextContent('Alpha');
    });
  });

  it('paginates non-empty slices', async () => {
    renderSUT({
      fetchSlices: () =>
        Promise.resolve({
          slices: Array.from({ length: 11 }, (_, index) => ({
            value: `Slice-${String(index + 1).padStart(2, '0')}`,
            count: 1,
          })),
        }),
    });

    await screen.findByText('Slice-01');

    expect(within(screen.getByTestId('slices-list')).getByText('Slice-01')).toBeInTheDocument();
    expect(within(screen.getByTestId('slices-list')).queryByText('Slice-11')).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: /open page 2/i }));

    await waitFor(() => expect(within(screen.getByTestId('slices-list')).getByText('Slice-11')).toBeInTheDocument());
    expect(within(screen.getByTestId('slices-list')).queryByText('Slice-01')).not.toBeInTheDocument();
  });

  it('paginates empty slices', async () => {
    renderSUT({
      fetchSlices: () =>
        Promise.resolve({
          slices: [
            { value: 'Alpha', count: 1 },
            ...Array.from({ length: 11 }, (_, index) => ({
              value: `Empty-${String(index + 1).padStart(2, '0')}`,
              count: 0,
            })),
          ],
        }),
    });

    await screen.findByText('Alpha');
    await userEvent.click(screen.getByRole('button', { name: /show empty slices/i }));

    expect(within(screen.getByTestId('empty-slices-list')).getByText('Empty-01')).toBeInTheDocument();
    expect(within(screen.getByTestId('empty-slices-list')).queryByText('Empty-11')).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: /open page 2/i }));

    await waitFor(() =>
      expect(within(screen.getByTestId('empty-slices-list')).getByText('Empty-11')).toBeInTheDocument(),
    );
    expect(within(screen.getByTestId('empty-slices-list')).queryByText('Empty-01')).not.toBeInTheDocument();
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

  it('auto-expands empty slices when active slice becomes empty', async () => {
    renderSUT(
      {
        fetchSlices: () =>
          Promise.resolve({
            slices: [
              { value: 'Alpha', count: 1 },
              { value: 'Gamma', count: 0 },
            ],
          }),
      },
      { searchParams: { slice: 'Gamma' } },
    );

    await screen.findByText('Alpha');

    expect(screen.getByRole('button', { name: /hide empty slices/i })).toBeInTheDocument();
    expect(within(screen.getByTestId('empty-slices-list')).getByText('Gamma')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: /hide empty slices/i }));

    expect(screen.getByRole('button', { name: /show empty slices/i })).toBeInTheDocument();
    expect(screen.queryByTestId('empty-slices-list')).not.toBeInTheDocument();
  });

  it('shows selected slice in empty list when backend does not return it', async () => {
    renderSUT(
      {
        fetchSlices: () =>
          Promise.resolve({
            slices: [{ value: 'Alpha', count: 1 }],
          }),
      },
      { searchParams: { slice: 'Missing-Slice' } },
    );

    await screen.findByText('Alpha');

    expect(screen.getByRole('button', { name: /hide empty slices/i })).toBeInTheDocument();
    expect(within(screen.getByTestId('empty-slices-list')).getByText('Missing-Slice')).toBeInTheDocument();
  });

  it('does not show selected slice when it does not match active filter', async () => {
    renderSUT(
      {
        fetchSlices: () =>
          Promise.resolve({
            slices: [{ value: 'Alpha', count: 1 }],
          }),
      },
      { searchParams: { slice: 'Missing-Slice' } },
    );

    await screen.findByText('Alpha');
    expect(within(screen.getByTestId('empty-slices-list')).getByText('Missing-Slice')).toBeInTheDocument();

    await userEvent.type(screen.getByPlaceholderText(/filter/i), 'alp');

    await waitFor(() => expect(screen.queryByText('Missing-Slice')).not.toBeInTheDocument());
    expect(screen.getByText('Empty slices (0)')).toBeInTheDocument();
  });

  it('refetches slices when selecting a slice', async () => {
    const fetchSlices = jest.fn(() =>
      Promise.resolve({
        slices: [
          { value: 'Alpha', count: 2 },
          { value: 'Beta', count: 1 },
        ],
      }),
    );
    renderSUT({ fetchSlices });

    await screen.findByText('Alpha');
    expect(fetchSlices).toHaveBeenCalledTimes(1);

    await userEvent.click(within(screen.getByTestId('slices-list')).getByRole('button', { name: /beta/i }));

    await waitFor(() => expect(fetchSlices.mock.calls.length).toBeGreaterThanOrEqual(2));
  });
});

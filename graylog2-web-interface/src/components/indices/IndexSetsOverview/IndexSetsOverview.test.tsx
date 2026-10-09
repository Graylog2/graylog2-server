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
import * as Immutable from 'immutable';
import type { Permission } from 'graylog-web-plugin/plugin';
import { render, screen, waitFor, within } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';
import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import { indexSets } from 'fixtures/indexSets';
import { layoutPreferences } from 'fixtures/entityListLayoutPreferences';
import { asMock } from 'helpers/mocking';
import useFetchEntities from 'components/common/PaginatedEntityTable/useFetchEntities';
import useUserLayoutPreferences from 'components/common/EntityDataTable/hooks/useUserLayoutPreferences';
import useProfile from 'components/indices/IndexSetFieldTypeProfiles/hooks/useProfile';
import useCurrentUser from 'hooks/useCurrentUser';
import { adminUser } from 'fixtures/users';
import useIndexSetMutations from 'components/indices/IndexSetsOverview/hooks/useIndexSetMutations';
import useIndexSetCategoryCounts from 'components/indices/IndexSetsOverview/hooks/useIndexSetCategoryCounts';
import useIndexSetMetrics from 'components/indices/IndexSetsOverview/hooks/useIndexSetMetrics';
import { cycleActiveWriteIndex, recalculateIndexRanges } from 'components/indices/helpers/indexSetMaintenanceActions';

import IndexSetsOverview from './IndexSetsOverview';
import type { IndexSetEntity } from './types';

jest.mock('components/common/PaginatedEntityTable/useFetchEntities');
jest.mock('components/common/EntityDataTable/hooks/useUserLayoutPreferences');
jest.mock('components/indices/IndexSetFieldTypeProfiles/hooks/useProfile');
jest.mock('hooks/useCurrentUser');
jest.mock('components/indices/helpers/indexSetMaintenanceActions');
jest.mock('api/streams', () => ({
  ...jest.requireActual('api/streams'),
  fetchStreams: jest.fn(() => Promise.resolve([])),
}));
jest.mock('components/indices/IndexSetsOverview/hooks/useIndexSetMutations');
jest.mock('components/indices/IndexSetsOverview/hooks/useIndexSetCategoryCounts');
jest.mock('components/indices/IndexSetsOverview/hooks/useIndexSetMetrics');
jest.mock('components/indices/IndicesConfiguration', () => ({
  __esModule: true,
  default: ({ indexSet }: { indexSet: IndexSetEntity }) => <span>Rotation and retention of {indexSet.title}</span>,
}));

const [defaultIndexSet, exampleIndexSet] = indexSets.map(
  (indexSet): IndexSetEntity => ({
    ...indexSet,
    id: indexSet.id,
    category: 'user',
    can_have_profile: true,
    stream_count: 0,
  }),
);
const readOnlyIndexSet: IndexSetEntity = {
  ...exampleIndexSet,
  id: 'index-set-id-3',
  title: 'Read-only index set',
  index_prefix: 'readonly',
  writable: false,
  can_be_default: false,
  field_type_profile: 'profile-id-1',
  stream_count: 12,
};

const setDefaultIndexSet = jest.fn();
const refetchMetrics = jest.fn();

const restrictedUser = adminUser
  .toBuilder()
  .permissions(Immutable.List(['indexsets:read', 'indexsets:edit', 'indexranges:rebuild'] as Array<Permission>))
  .build();

const attributes = [
  { id: 'title', title: 'Title', sortable: true },
  { id: 'description', title: 'Description', sortable: false },
  { id: 'index_prefix', title: 'Index prefix', sortable: true },
  { id: 'shards', title: 'Shards', type: 'INT' as const, sortable: true },
  { id: 'replicas', title: 'Replicas', type: 'INT' as const, sortable: true },
  {
    id: 'category',
    title: 'Category',
    sortable: false,
    filterable: true,
    filter_options: [
      { value: 'user', title: 'User' },
      { value: 'system', title: 'System' },
      { value: 'illuminate', title: 'Illuminate' },
    ],
  },
  { id: 'field_type_profile', title: 'Field type profile', sortable: true },
  { id: 'stream_count', title: 'Streams', type: 'INT' as const, sortable: true },
];

const mockIndexSets = (list: Array<IndexSetEntity> = [defaultIndexSet, exampleIndexSet, readOnlyIndexSet]) =>
  asMock(useFetchEntities).mockReturnValue({
    data: { pagination: { total: list.length }, list, attributes },
    refetch: () => {},
    isInitialLoading: false,
  });

const mockMetrics = (overrides: Partial<ReturnType<typeof useIndexSetMetrics>> = {}) =>
  asMock(useIndexSetMetrics).mockReturnValue({
    metricsByIndexSetId: {
      [exampleIndexSet.id]: {
        index_count: 1234,
        document_count: 1234567,
        size_bytes: 12582912,
        deflector_health: 'Yellow',
      },
      [readOnlyIndexSet.id]: { index_count: 3, size_bytes: 0, deflector_health: null },
    },
    isLoading: false,
    isError: false,
    refetch: refetchMetrics,
    ...overrides,
  });

const lastSearchParams = () => asMock(useFetchEntities).mock.lastCall[0].searchParams;

const findRow = (indexSet: IndexSetEntity) => screen.findByTestId(`table-row-${indexSet.id}`);

const openMoreActions = async (indexSet: IndexSetEntity) =>
  userEvent.click(within(await findRow(indexSet)).getByRole('button', { name: /more/i }));

describe('IndexSetsOverview', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    asMock(useUserLayoutPreferences).mockReturnValue({
      data: { ...layoutPreferences, attributes: undefined },
      isInitialLoading: false,
      refetch: () => {},
    });
    asMock(useCurrentUser).mockReturnValue(adminUser);
    asMock(useIndexSetMutations).mockReturnValue({ setDefaultIndexSet, deleteIndexSet: jest.fn() });
    asMock(useProfile).mockReturnValue({
      data: { id: 'profile-id-1', name: 'My Profile', description: null, customFieldMappings: [], indexSetIds: [] },
      isFetched: true,
      isFetching: false,
      refetch: () => {},
    });
    asMock(useIndexSetCategoryCounts).mockReturnValue({
      data: { all: 9, user: 5, system: 4, illuminate: 0 },
    });
    mockIndexSets();
    mockMetrics();
  });

  it('renders index sets with their status labels', async () => {
    render(<IndexSetsOverview />);

    within(await findRow(defaultIndexSet)).getByText('Default');
    within(await findRow(readOnlyIndexSet)).getByText('Read only');

    expect(within(await findRow(exampleIndexSet)).queryByText('Default')).not.toBeInTheDocument();
  });

  it('does not show associated streams in the default view', async () => {
    render(<IndexSetsOverview />);

    await screen.findByText('Description');

    expect(screen.queryByText('Streams')).not.toBeInTheDocument();
  });

  it('shows the index set count per category', async () => {
    render(<IndexSetsOverview />);

    await screen.findByRole('button', { name: /^all\s*9/i });
    await screen.findByRole('button', { name: /^user-defined\s*5/i });
    await screen.findByRole('button', { name: /^system\s*4/i });

    expect(screen.queryByRole('button', { name: /^illuminate/i })).not.toBeInTheDocument();
  });

  it('shows the illuminate category when illuminate index sets exist', async () => {
    asMock(useIndexSetCategoryCounts).mockReturnValue({
      data: { all: 11, user: 5, system: 4, illuminate: 2 },
    });
    render(<IndexSetsOverview />);

    await screen.findByRole('button', { name: /^illuminate\s*2/i });
  });

  it('filters index sets by category', async () => {
    render(<IndexSetsOverview />);

    await userEvent.click(await screen.findByRole('button', { name: /^user/i }));

    await waitFor(() => expect(lastSearchParams().filters.get('category')).toEqual(['user']));

    await userEvent.click(await screen.findByRole('button', { name: /^all/i }));

    await waitFor(() => expect(lastSearchParams().filters?.get('category')).toBeUndefined());
  });

  it('keeps the search placeholder', async () => {
    render(<IndexSetsOverview />);

    await screen.findByPlaceholderText('Find index sets');
  });

  it('links to a search for the messages stored in the index set', async () => {
    render(<IndexSetsOverview />);

    const searchLink = within(await findRow(exampleIndexSet)).getByRole('link', {
      name: /search in index set example index set/i,
    });

    expect(decodeURIComponent(searchLink.getAttribute('href'))).toContain('q=_index:example_*');
  });

  it('expands the details of an index set', async () => {
    render(<IndexSetsOverview />);

    await userEvent.click(within(await findRow(exampleIndexSet)).getByText(exampleIndexSet.description));

    await screen.findByText('Rotation and retention of Example Index Set');
    await screen.findByText('Field type refresh interval:');
    await screen.findByText('5 seconds');
  });

  it('sets an index set as default', async () => {
    render(<IndexSetsOverview />);

    await openMoreActions(exampleIndexSet);
    await userEvent.click(await screen.findByRole('menuitem', { name: /set as default/i }));

    expect(setDefaultIndexSet).toHaveBeenCalledWith(exampleIndexSet);
  });

  it.each([
    ['the current default', defaultIndexSet],
    ['a set that cannot be default', readOnlyIndexSet],
  ])('disables set as default for %s', async (_description, indexSet) => {
    render(<IndexSetsOverview />);

    await openMoreActions(indexSet);

    expect(await screen.findByRole('menuitem', { name: /set as default/i })).toBeDisabled();
  });

  it('only offers rotation for writable index sets', async () => {
    render(<IndexSetsOverview />);

    await openMoreActions(readOnlyIndexSet);
    await screen.findByRole('menuitem', { name: /recalculate index ranges/i });

    expect(screen.queryByRole('menuitem', { name: /rotate active write index/i })).not.toBeInTheDocument();
  });

  it('hides rotation without the deflector cycle permission', async () => {
    asMock(useCurrentUser).mockReturnValue(restrictedUser);
    render(<IndexSetsOverview />);

    await openMoreActions(exampleIndexSet);
    await screen.findByRole('menuitem', { name: /recalculate index ranges/i });

    expect(screen.queryByRole('menuitem', { name: /rotate active write index/i })).not.toBeInTheDocument();
  });

  it('recalculates index ranges after confirmation', async () => {
    render(<IndexSetsOverview />);

    await openMoreActions(exampleIndexSet);
    await userEvent.click(await screen.findByRole('menuitem', { name: /recalculate index ranges/i }));

    await screen.findByText(/this will recalculate index ranges for this index set/i);

    expect(recalculateIndexRanges).not.toHaveBeenCalled();

    await userEvent.click(screen.getByRole('button', { name: /recalculate/i }));

    expect(recalculateIndexRanges).toHaveBeenCalledWith(exampleIndexSet.id);
  });

  it('rotates the active write index after confirmation', async () => {
    render(<IndexSetsOverview />);

    await openMoreActions(exampleIndexSet);
    await userEvent.click(await screen.findByRole('menuitem', { name: /rotate active write index/i }));

    await screen.findByText(/this will manually cycle the current active write index/i);

    expect(cycleActiveWriteIndex).not.toHaveBeenCalled();

    await userEvent.click(screen.getByRole('button', { name: /^rotate$/i }));

    expect(cycleActiveWriteIndex).toHaveBeenCalledWith(exampleIndexSet.id);
  });

  it('opens the deletion dialog', async () => {
    render(<IndexSetsOverview />);

    await openMoreActions(exampleIndexSet);
    await userEvent.click(await screen.findByRole('menuitem', { name: /delete/i }));

    await screen.findByText('Delete index set "Example Index Set"?');
  });

  it('shows configuration columns in the configuration view', async () => {
    render(<IndexSetsOverview />);

    await userEvent.click(await screen.findByRole('radio', { name: 'Configuration' }));

    expect(await screen.findAllByText('Field type profile')).toHaveLength(1);
    within(await findRow(readOnlyIndexSet)).getByRole('link', { name: 'My Profile' });
    within(await findRow(exampleIndexSet)).getByText('Not set');
  });

  it('removes configuration columns when switching back to the default view', async () => {
    render(<IndexSetsOverview />);

    await userEvent.click(await screen.findByRole('radio', { name: 'Configuration' }));
    await screen.findByText('Field type profile');

    await userEvent.click(await screen.findByRole('radio', { name: 'Default' }));

    await screen.findByText('Description');

    expect(screen.queryByText('Field type profile')).not.toBeInTheDocument();
    expect(within(await findRow(exampleIndexSet)).queryByText('Not set')).not.toBeInTheDocument();
  });

  it('shows associated streams in the routing view without requesting metrics', async () => {
    render(<IndexSetsOverview />);

    await userEvent.click(await screen.findByRole('radio', { name: 'Routing' }));

    await screen.findByText('Streams');
    within(await findRow(readOnlyIndexSet)).getByText('12');

    expect(screen.queryByText('Description')).not.toBeInTheDocument();
    expect(asMock(useIndexSetMetrics).mock.lastCall[1]).toEqual([]);
  });

  it('does not load profiles the user may not read', async () => {
    asMock(useCurrentUser).mockReturnValue(restrictedUser);
    render(<IndexSetsOverview />);

    await userEvent.click(await screen.findByRole('radio', { name: 'Configuration' }));

    within(await findRow(readOnlyIndexSet)).getByText('profile-id-1');

    expect(useProfile).not.toHaveBeenCalledWith('profile-id-1');
  });

  it('requests metrics of the visible index sets for the default view columns', async () => {
    render(<IndexSetsOverview />);

    await waitFor(() =>
      expect(useIndexSetMetrics).toHaveBeenLastCalledWith(
        [defaultIndexSet.id, exampleIndexSet.id, readOnlyIndexSet.id],
        ['index_count', 'size_bytes'],
      ),
    );
  });

  it('requests the field count in the configuration view', async () => {
    render(<IndexSetsOverview />);

    await userEvent.click(await screen.findByRole('radio', { name: 'Configuration' }));

    await waitFor(() => expect(asMock(useIndexSetMetrics).mock.lastCall[1]).toEqual(['field_count']));
  });

  it('shows the metrics of each index set', async () => {
    render(<IndexSetsOverview />);

    const exampleRow = await findRow(exampleIndexSet);
    within(exampleRow).getByText('1,234');

    expect(within(exampleRow).getByText('12.0MiB')).toBeInTheDocument();
  });

  it('abbreviates the document count', async () => {
    asMock(useUserLayoutPreferences).mockReturnValue({
      data: {
        ...layoutPreferences,
        attributes: { title: { status: 'show' }, document_count: { status: 'show' } },
      },
      isInitialLoading: false,
      refetch: () => {},
    });
    render(<IndexSetsOverview />);

    expect(await within(await findRow(exampleIndexSet)).findByText('1.2m')).toBeInTheDocument();
  });

  it('shows a spinner while metrics are loading', async () => {
    mockMetrics({ metricsByIndexSetId: {}, isLoading: true });
    render(<IndexSetsOverview />);

    expect(await within(await findRow(exampleIndexSet)).findAllByText(/loading/i)).toHaveLength(2);
  });

  it('keeps loaded metrics while the missing ones are loading', async () => {
    mockMetrics({ metricsByIndexSetId: { [exampleIndexSet.id]: { index_count: 1234 } }, isLoading: true });
    render(<IndexSetsOverview />);

    const exampleRow = await findRow(exampleIndexSet);
    within(exampleRow).getByText('1,234');

    expect(await within(exampleRow).findAllByText(/loading/i)).toHaveLength(1);
  });

  it('does not offer the deflector health column', async () => {
    render(<IndexSetsOverview />);

    await screen.findByText('Title');

    expect(screen.queryByText('Deflector')).not.toBeInTheDocument();
  });

  it('offers a retry when metrics could not be loaded', async () => {
    mockMetrics({ metricsByIndexSetId: {}, isError: true });
    render(<IndexSetsOverview />);

    await userEvent.click(
      within(await findRow(exampleIndexSet)).getByRole('button', {
        name: 'Retry loading indices for index set Example Index Set',
      }),
    );

    expect(refetchMetrics).toHaveBeenCalled();
  });

  it('keeps showing loaded metrics when refreshing them fails', async () => {
    mockMetrics({ isError: true });
    render(<IndexSetsOverview />);

    within(await findRow(exampleIndexSet)).getByText('1,234');

    expect(
      screen.queryByRole('button', { name: /retry loading indices for index set example/i }),
    ).not.toBeInTheDocument();
  });

  it('does not offer sorting by metric columns', async () => {
    render(<IndexSetsOverview />);

    await screen.findByRole('button', { name: /sort title/i });

    expect(screen.queryByRole('button', { name: /sort indices/i })).not.toBeInTheDocument();
  });

  it('adds plugin columns to the configuration view', async () => {
    const plugin = new PluginManifest(
      {},
      {
        'components.indexSets.overview.tableElements': [
          {
            attributeName: 'plugin_column',
            group: 'configuration',
            attributes: [{ id: 'plugin_column', title: 'Plugin Column' }],
            columnRenderers: {
              plugin_column: { renderCell: () => 'plugin cell', staticWidth: 'matchHeader' },
            },
          },
        ],
      },
    );
    PluginStore.register(plugin);

    try {
      render(<IndexSetsOverview />);

      await screen.findByText('Title');

      expect(screen.queryByText('Plugin Column')).not.toBeInTheDocument();

      await userEvent.click(await screen.findByRole('radio', { name: 'Configuration' }));

      await screen.findByText('Plugin Column');
      within(await findRow(exampleIndexSet)).getByText('plugin cell');
    } finally {
      PluginStore.unregister(plugin);
    }
  });
});

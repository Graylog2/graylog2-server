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
import { Formik } from 'formik';
import { render, screen } from 'wrappedTestingLibrary';

import { asMock } from 'helpers/mocking';
import selectEvent from 'helpers/selectEvent';
import EntityShareDomain from 'domainActions/permissions/EntityShareDomain';
import SharedEntity from 'logic/permissions/SharedEntity';
import useDashboards from 'views/components/dashboard/hooks/useDashboards';
import useStreams from 'components/streams/hooks/useStreams';
import useSavedSearches from 'views/hooks/useSavedSearches';
import usePluggableLicenseCheck from 'hooks/usePluggableLicenseCheck';

import StartpageFormGroup from './StartpageFormGroup';

jest.mock('views/components/dashboard/hooks/useDashboards');
jest.mock('components/streams/hooks/useStreams');
jest.mock('views/hooks/useSavedSearches');
jest.mock('hooks/usePluggableLicenseCheck');
jest.mock('domainActions/permissions/EntityShareDomain', () => ({
  loadUserSharesPaginated: jest.fn(),
}));

const listResult = (list: Array<{ id: string; title: string }>) =>
  ({
    data: { list, pagination: { total: list.length }, attributes: [] },
    isInitialLoading: false,
  }) as any;

const sharedEntity = (type: string, id: string, title: string) =>
  SharedEntity.builder().id(`grn::::${type}:${id}`).type(type).title(title).owners(Immutable.List()).build();

const mockShares = (shares: { [entityType: string]: Array<SharedEntity> } = {}) =>
  asMock(EntityShareDomain.loadUserSharesPaginated).mockImplementation((_userId, { additionalQueries }) =>
    Promise.resolve({ list: Immutable.List(shares[additionalQueries.entity_type] ?? []) } as any),
  );

const renderPicker = (permissions: Array<string>, startpage: { type: string; id?: string }) =>
  render(
    <Formik initialValues={{ startpage }} onSubmit={() => {}}>
      <StartpageFormGroup userId="user-id" permissions={Immutable.List(permissions)} />
    </Formik>,
  );

const openOptions = async (type: string) => {
  selectEvent.openMenu(await selectEvent.findSelectInput(`Select ${type}`));

  return screen.findAllByRole('option').then((options) => options.map((option) => option.textContent));
};

describe('StartpageFormGroup', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    asMock(useDashboards).mockReturnValue(
      listResult([
        { id: 'dashboard-1', title: 'Dashboard 1' },
        { id: 'dashboard-2', title: 'Dashboard 2' },
      ]),
    );
    asMock(useStreams).mockReturnValue(
      listResult([
        { id: 'stream-1', title: 'Stream 1' },
        { id: 'stream-2', title: 'Stream 2' },
      ]),
    );
    asMock(useSavedSearches).mockReturnValue(
      listResult([
        { id: 'search-1', title: 'Search 1' },
        { id: 'search-2', title: 'Search 2' },
      ]),
    );
    asMock(usePluggableLicenseCheck).mockReturnValue({ data: { valid: false } } as any);
    mockShares();
  });

  it('lists streams the user can read through a role permission', async () => {
    renderPicker(['streams:read'], { type: 'stream' });

    expect(await openOptions('stream')).toEqual(['Stream 1', 'Stream 2']);
  });

  it('lists only dashboards the user can read through entity permissions', async () => {
    renderPicker(['view:read:dashboard-1'], { type: 'dashboard' });

    expect(await openOptions('dashboard')).toEqual(['Dashboard 1']);
  });

  it('accepts legacy dashboard read permissions', async () => {
    renderPicker(['dashboards:read:dashboard-2'], { type: 'dashboard' });

    expect(await openOptions('dashboard')).toEqual(['Dashboard 2']);
  });

  it('lists searches the user can read', async () => {
    renderPicker(['view:read:search-2'], { type: 'search' });

    expect(await openOptions('search')).toEqual(['New Search', 'Search 2']);
  });

  it('includes shared entities once', async () => {
    mockShares({
      stream: [sharedEntity('stream', 'stream-1', 'Stream 1'), sharedEntity('stream', 'stream-3', 'Stream 3')],
    });
    renderPicker(['streams:read:stream-1'], { type: 'stream' });

    expect(await openOptions('stream')).toEqual(['Stream 1', 'Stream 3']);
  });

  it('lists everything for admins without loading shares', async () => {
    renderPicker(['*'], { type: 'stream' });

    expect(await openOptions('stream')).toEqual(['Stream 1', 'Stream 2']);
    expect(EntityShareDomain.loadUserSharesPaginated).not.toHaveBeenCalled();
  });

  it('warns only when the user cannot read the configured start page', async () => {
    const { unmount } = renderPicker(['streams:read'], { type: 'stream', id: 'stream-2' });

    await selectEvent.findSelectInput('Select stream');

    expect(screen.queryByText(/user is missing permission/i)).not.toBeInTheDocument();

    unmount();
    renderPicker(['streams:read:stream-1'], { type: 'stream', id: 'stream-2' });

    expect(await screen.findByText(/user is missing permission/i)).toBeInTheDocument();
  });
});

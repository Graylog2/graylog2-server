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
import { render, screen } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';
import type { Permission } from 'graylog-web-plugin/plugin';

import asMock from 'helpers/mocking/AsMock';
import useSelectedEntities from 'components/common/EntityDataTable/hooks/useSelectedEntities';
import useCurrentUser from 'hooks/useCurrentUser';
import { adminUser } from 'fixtures/users';
import { useFleets } from 'components/collectors/hooks';

import BulkActions from './BulkActions';

jest.mock('components/common/EntityDataTable/hooks/useSelectedEntities');
jest.mock('hooks/useCurrentUser');
jest.mock('components/collectors/hooks', () => ({
  __esModule: true,
  ...jest.requireActual('components/collectors/hooks'),
  useFleets: jest.fn(),
}));
jest.mock('./ReassignFleetModal', () => (props: { onClose: () => void; onSuccess: () => void }) => (
  <div data-testid="reassign-modal">
    <button type="button" onClick={props.onClose}>
      Close
    </button>
    <button type="button" onClick={props.onSuccess}>
      Succeed
    </button>
  </div>
));

const userWith = (permissions: Array<string>) =>
  adminUser.toBuilder().permissions(Immutable.List(permissions as Array<Permission>)).build();

const setSelectedEntitiesMock = jest.fn();

const useSelectedEntitiesResponse = {
  selectedEntities: [],
  setSelectedEntities: setSelectedEntitiesMock,
  selectEntity: () => {},
  deselectEntity: () => {},
  toggleEntitySelect: () => {},
  isSomeRowsSelected: false,
  isAllRowsSelected: false,
};

const openActionsDropdown = async () => {
  await userEvent.click(await screen.findByRole('button', { name: /bulk actions/i }));
};

describe('BulkActions', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    asMock(useSelectedEntities).mockReturnValue(useSelectedEntitiesResponse);
    asMock(useFleets).mockReturnValue({ data: [{ id: 'f-1', name: 'Fleet 1' }], isLoading: false } as never);
    asMock(useCurrentUser).mockReturnValue(
      userWith(['collector_fleets:read:f-1', 'collector_fleets:assign_instance:f-1']),
    );
  });

  it('renders bulk actions dropdown', async () => {
    render(<BulkActions />);

    await screen.findByRole('button', { name: /bulk actions/i });
  });

  it('is disabled when no entities are selected', async () => {
    render(<BulkActions />);

    const button = await screen.findByRole('button', { name: /bulk actions/i });

    expect(button).toBeDisabled();
  });

  it('is enabled when entities are selected', async () => {
    asMock(useSelectedEntities).mockReturnValue({
      ...useSelectedEntitiesResponse,
      selectedEntities: ['uid-1', 'uid-2'],
    });

    render(<BulkActions />);

    const button = await screen.findByRole('button', { name: /bulk actions/i });

    expect(button).not.toBeDisabled();
  });

  it('shows Reassign to fleet menu item', async () => {
    asMock(useSelectedEntities).mockReturnValue({
      ...useSelectedEntitiesResponse,
      selectedEntities: ['uid-1'],
    });

    render(<BulkActions />);

    await openActionsDropdown();

    await screen.findByRole('menuitem', { name: /reassign to fleet/i });
  });

  it('opens reassign modal when Reassign to fleet is clicked', async () => {
    asMock(useSelectedEntities).mockReturnValue({
      ...useSelectedEntitiesResponse,
      selectedEntities: ['uid-1'],
    });

    render(<BulkActions />);

    await openActionsDropdown();
    await userEvent.click(await screen.findByRole('menuitem', { name: /reassign to fleet/i }));

    await screen.findByTestId('reassign-modal');
  });

  it('clears selection on reassign success', async () => {
    asMock(useSelectedEntities).mockReturnValue({
      ...useSelectedEntitiesResponse,
      selectedEntities: ['uid-1'],
    });

    render(<BulkActions />);

    await openActionsDropdown();
    await userEvent.click(await screen.findByRole('menuitem', { name: /reassign to fleet/i }));
    await userEvent.click(await screen.findByRole('button', { name: /succeed/i }));

    expect(setSelectedEntitiesMock).toHaveBeenCalledWith([]);
  });

  it('hides the dropdown when the user cannot assign into any fleet', async () => {
    asMock(useCurrentUser).mockReturnValue(userWith(['collector_fleets:read:f-1']));

    render(<BulkActions />);

    expect(screen.queryByRole('button', { name: /bulk actions/i })).not.toBeInTheDocument();
  });
});

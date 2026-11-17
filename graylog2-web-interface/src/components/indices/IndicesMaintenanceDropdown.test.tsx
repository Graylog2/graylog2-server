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

import asMock from 'helpers/mocking/AsMock';
import usePermissions from 'hooks/usePermissions';
import type { IndexSet } from 'stores/indices/IndexSetsStore';
import { cycleActiveWriteIndex, saveIndexSetFieldLimit } from 'components/indices/helpers/indexSetMaintenanceActions';

import IndicesMaintenanceDropdown from './IndicesMaintenanceDropdown';

jest.mock('hooks/usePermissions');
jest.mock('components/indices/helpers/indexSetMaintenanceActions', () => ({
  recalculateIndexRanges: jest.fn(),
  cycleActiveWriteIndex: jest.fn(),
  saveIndexSetFieldLimit: jest.fn(),
}));

const indexSet = { id: 'index-set-id', title: 'Index set', writable: true, field_limit: 1000 } as IndexSet;

const mockPermissions = (granted: Array<string>) =>
  asMock(usePermissions).mockReturnValue({
    isPermitted: (permission) => granted.includes(permission as string),
    isAnyPermitted: () => false,
    isAdmin: false,
  });

const openItem = async () => {
  await userEvent.click(await screen.findByRole('button', { name: /maintenance/i }));

  return screen.findByRole('menuitem', { name: /raise max field count and rotate/i });
};

describe('IndicesMaintenanceDropdown raise max field count', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockPermissions(['indexsets:edit:index-set-id', 'deflector:cycle']);
    asMock(saveIndexSetFieldLimit).mockResolvedValue(indexSet);
    asMock(cycleActiveWriteIndex).mockResolvedValue(undefined);
  });

  it('shows the item for writable index sets and permitted users', async () => {
    render(<IndicesMaintenanceDropdown indexSet={indexSet} indexSetId={indexSet.id} />);

    expect(await openItem()).toBeInTheDocument();
  });

  it('hides the item for index sets that are not writable', async () => {
    render(<IndicesMaintenanceDropdown indexSet={{ ...indexSet, writable: false }} indexSetId={indexSet.id} />);

    await userEvent.click(await screen.findByRole('button', { name: /maintenance/i }));

    expect(screen.queryByRole('menuitem', { name: /raise max field count and rotate/i })).not.toBeInTheDocument();
  });

  it('hides the item without the permission to rotate', async () => {
    mockPermissions(['indexsets:edit:index-set-id']);
    render(<IndicesMaintenanceDropdown indexSet={indexSet} indexSetId={indexSet.id} />);

    await userEvent.click(await screen.findByRole('button', { name: /maintenance/i }));

    expect(screen.queryByRole('menuitem', { name: /raise max field count and rotate/i })).not.toBeInTheDocument();
  });

  it('saves the limit, then rotates', async () => {
    render(<IndicesMaintenanceDropdown indexSet={indexSet} indexSetId={indexSet.id} />);

    await userEvent.click(await openItem());

    const input = await screen.findByRole('spinbutton', { name: /max field count/i });

    expect(input).toHaveValue(1000);

    await userEvent.clear(input);
    await userEvent.type(input, '1500');
    await userEvent.click(screen.getByRole('button', { name: /save and rotate/i }));

    await screen.findByRole('button', { name: /maintenance/i });

    expect(saveIndexSetFieldLimit).toHaveBeenCalledWith(indexSet, 1500);
    expect(cycleActiveWriteIndex).toHaveBeenCalledWith('index-set-id');
  });

  it('does not rotate when saving fails', async () => {
    asMock(saveIndexSetFieldLimit).mockRejectedValue({
      additional: { body: [{ path: 'field_limit', message: 'must be between 1000 and 2000' }] },
    });
    render(<IndicesMaintenanceDropdown indexSet={indexSet} indexSetId={indexSet.id} />);

    await userEvent.click(await openItem());
    await userEvent.click(await screen.findByRole('button', { name: /save and rotate/i }));

    expect(await screen.findByText(/must be between 1000 and 2000/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /save and rotate/i })).toBeDisabled();
    expect(cycleActiveWriteIndex).not.toHaveBeenCalled();

    await userEvent.type(screen.getByRole('spinbutton', { name: /max field count/i }), '0');

    expect(screen.getByRole('button', { name: /save and rotate/i })).toBeEnabled();
  });

  it('keeps the saved limit and offers a retry when the rotation fails', async () => {
    asMock(cycleActiveWriteIndex).mockRejectedValueOnce(new Error('rotation failed'));
    render(<IndicesMaintenanceDropdown indexSet={indexSet} indexSetId={indexSet.id} />);

    await userEvent.click(await openItem());
    await userEvent.click(await screen.findByRole('button', { name: /save and rotate/i }));

    expect(
      await screen.findByText(/max field count is saved, but the index set could not be rotated/i),
    ).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: /retry rotation/i }));

    await screen.findByRole('button', { name: /maintenance/i });

    expect(saveIndexSetFieldLimit).toHaveBeenCalledTimes(1);
    expect(cycleActiveWriteIndex).toHaveBeenCalledTimes(2);
  });
});

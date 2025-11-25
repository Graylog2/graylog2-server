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

import React from 'react';
import type { Column, Table } from '@tanstack/react-table';
import userEvent from '@testing-library/user-event';
import { render, screen, waitFor } from 'wrappedTestingLibrary';

import type { EntityBase } from 'components/common/EntityDataTable/types';

import ColumnsVisibilitySelect from './ColumnsVisibilitySelect';

const createColumn = (id: string, label?: string): Column<Record<string, unknown>> =>
  ({
    id,
    columnDef: { meta: { label: label ?? id } },
    getCanHide: () => true,
    getIsVisible: () => true,
    toggleVisibility: jest.fn(),
  }) as unknown as Column<Record<string, unknown>>;

describe('<ColumnsVisibilitySelect />', () => {
  it('resets layout preferences and internal state when clicking reset all columns', async () => {
    const defaultColumnOrder = ['title', 'status'];
    const onLayoutPreferencesChange = jest.fn(() => Promise.resolve());
    const setInternalAttributeColumnOrder = jest.fn();
    const setInternalColumnWidthPreferences = jest.fn();
    const table = {
      getAllLeafColumns: () => [createColumn('title', 'Title'), createColumn('status', 'Status')],
    } as unknown as Table<EntityBase>;

    render(
      <ColumnsVisibilitySelect
        defaultColumnOrder={defaultColumnOrder}
        onLayoutPreferencesChange={onLayoutPreferencesChange}
        setInternalAttributeColumnOrder={setInternalAttributeColumnOrder}
        setInternalColumnWidthPreferences={setInternalColumnWidthPreferences}
        table={table}
      />,
    );

    userEvent.click(await screen.findByRole('button', { name: /configure visible columns/i }));
    userEvent.click(await screen.findByRole('menuitem', { name: /reset all columns/i }));

    await waitFor(() => expect(onLayoutPreferencesChange).toHaveBeenCalledWith({ attributes: null, order: null }));
    expect(setInternalAttributeColumnOrder).toHaveBeenCalledWith(defaultColumnOrder);
    expect(setInternalColumnWidthPreferences).toHaveBeenCalledWith({});
  });
});

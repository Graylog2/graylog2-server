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

import { useMemo, useCallback } from 'react';
import type { ColumnDef, SortingState, Updater, VisibilityState, RowSelectionState } from '@tanstack/react-table';
import { getCoreRowModel, useReactTable } from '@tanstack/react-table';

import type { Sort } from 'stores/PaginationTypes';

import type { ColumnPreferences, EntityBase } from '../types';

const updateColumnPreferences = (
  prevVisibleColumns: VisibilityState,
  currVisibleColumns: VisibilityState,
  columnPreferences: ColumnPreferences | undefined = {},
) => {
  const updated = { ...columnPreferences };

  // only update the preferences for columns which have been shown/hidden by the user
  Object.keys(currVisibleColumns).forEach((key) => {
    if (currVisibleColumns[key] && !prevVisibleColumns[key]) {
      updated[key] = { status: 'show' };
    } else if (!currVisibleColumns[key] && prevVisibleColumns[key]) {
      updated[key] = { status: 'hide' };
    }
  });

  return updated;
};

type Props<Entity extends EntityBase> = {
  columnPreferences: ColumnPreferences | undefined;
  columnsDefinitions: Array<ColumnDef<Entity>>;
  displayBulkSelectCol: boolean;
  entities: ReadonlyArray<Entity>;
  isEntitySelectable: (entity: Entity) => boolean | undefined;
  onColumnPreferencesChange: (newColumnPreferences: ColumnPreferences) => void;
  onSortChange: (sort: Sort) => void;
  sort: Sort | undefined;
  visibleColumnOrder: Array<string>;
  selectedEntities: Array<Entity['id']>;
  setSelectedEntities: any;
  onChangeSelection: (selectedEntities: Array<Entity['id']>, data: Readonly<Array<Entity>>) => void;
};

const useTable = <Entity extends EntityBase>({
  columnPreferences,
  columnsDefinitions,
  displayBulkSelectCol,
  entities,
  isEntitySelectable = () => true,
  onChangeSelection,
  onColumnPreferencesChange,
  onSortChange,
  selectedEntities,
  setSelectedEntities,
  sort,
  visibleColumnOrder,
}: Props<Entity>) => {
  const data = useMemo(() => [...entities], [entities]);
  const sorting = useMemo(() => (sort ? [{ id: sort.attributeId, desc: sort.direction === 'desc' }] : []), [sort]);

  const onSortingChange = useCallback(
    (updater: Updater<SortingState>) => {
      const newSorting = updater instanceof Function ? updater(sorting) : updater;
      onSortChange({ attributeId: newSorting[0].id, direction: newSorting[0].desc ? 'desc' : 'asc' });
    },
    [onSortChange, sorting],
  );

  const columnVisibility = useMemo(
    () => Object.fromEntries(columnsDefinitions.map(({ id }) => [id, visibleColumnOrder.includes(id)])),
    [columnsDefinitions, visibleColumnOrder],
  );

  const onColumnVisibilityChange = useCallback(
    (updater: Updater<VisibilityState>) => {
      const newColumnVisibility = updater instanceof Function ? updater(columnVisibility) : updater;

      return onColumnPreferencesChange(
        updateColumnPreferences(columnVisibility, newColumnVisibility, columnPreferences),
      );
    },
    [columnPreferences, columnVisibility, onColumnPreferencesChange],
  );

  const rowSelection = useMemo(() => Object.fromEntries(selectedEntities.map((id) => [id, true])), [selectedEntities]);

  const onRowSelectionChange = useCallback(
    (updater: Updater<RowSelectionState>) => {
      const newRowSelection = updater instanceof Function ? updater(rowSelection) : updater;

      const newSelection = Object.entries(newRowSelection)
        .filter(([_id, isSelected]) => isSelected)
        .map(([id]) => id);

      setSelectedEntities(newSelection);

      if (onChangeSelection) {
        onChangeSelection(newSelection, entities);
      }
    },
    [entities, onChangeSelection, rowSelection, setSelectedEntities],
  );

  // eslint-disable-next-line react-hooks/incompatible-library
  return useReactTable({
    columns: columnsDefinitions,
    data,
    enableRowSelection: (row) => displayBulkSelectCol && isEntitySelectable(row.original),
    enableSortingRemoval: false,
    getCoreRowModel: getCoreRowModel(),
    getRowId: (row) => row.id,
    manualSorting: true,
    onColumnOrderChange: () => {},
    onColumnVisibilityChange,
    onRowSelectionChange,
    onSortingChange,
    state: {
      columnOrder: visibleColumnOrder,
      columnVisibility,
      sorting,
      rowSelection,
    },
  });
};

export default useTable;

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
import type { ColumnDef, SortingState, Updater, VisibilityState } from '@tanstack/react-table';
import { getCoreRowModel, useReactTable } from '@tanstack/react-table';

import type { EntityBase } from 'components/common/EntityDataTable/types';
import type { Sort } from 'stores/PaginationTypes';
import { BULK_SELECT_COL_ID, ACTIONS_COL_ID, UTILITY_COLUMNS } from 'components/common/EntityDataTable/Constants';

const useComputedColumnOrder = (visibleColumns: Array<string>, attributeColumnsOder: Array<string>) =>
  useMemo(() => {
    const visibleSet = new Set(visibleColumns);
    const coreOrder = attributeColumnsOder.filter((id) => visibleSet.has(id));
    // Display columns, which are not part of the defined order, at the end of the table (before actions column)
    const additionalVisible = visibleColumns.filter(
      (id) => !UTILITY_COLUMNS.has(id) && !attributeColumnsOder.includes(id),
    );

    return [
      visibleSet.has(BULK_SELECT_COL_ID) ? BULK_SELECT_COL_ID : null,
      ...coreOrder,
      ...additionalVisible,
      visibleSet.has(ACTIONS_COL_ID) ? ACTIONS_COL_ID : null,
    ].filter(Boolean);
  }, [visibleColumns, attributeColumnsOder]);

type Props<Entity extends EntityBase> = {
  columns: Array<ColumnDef<Entity>>;
  attributeColumnsOder: Array<string>;
  entities: ReadonlyArray<Entity>;
  isEntitySelectable: (entity: Entity) => boolean;
  onColumnsChange: (visibleColumns: Array<string>) => void;
  onSortChange: (sort: Sort) => void;
  sort: Sort | undefined;
  visibleColumns: Array<string>;
};

const useTable = <Entity extends EntityBase>({
  columns,
  attributeColumnsOder,
  entities,
  isEntitySelectable,
  onColumnsChange,
  onSortChange,
  sort,
  visibleColumns,
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
    () => Object.fromEntries(columns.map(({ id }) => [id, visibleColumns.includes(id)])),
    [columns, visibleColumns],
  );

  const columnOrder = useComputedColumnOrder(visibleColumns, attributeColumnsOder);

  const onColumnVisibilityChange = useCallback(
    (updater: Updater<VisibilityState>) => {
      const newColumnVisibility = updater instanceof Function ? updater(columnVisibility) : updater;

      return onColumnsChange(
        Object.entries(newColumnVisibility)
          .filter(([_colId, isVisible]) => isVisible)
          .map(([colId]) => colId)
          .filter((colId) => !UTILITY_COLUMNS.has(colId)),
      );
    },
    [columnVisibility, onColumnsChange],
  );

  return useReactTable({
    data,
    columns,
    getCoreRowModel: getCoreRowModel(),
    manualSorting: true,
    enableSortingRemoval: false,
    enableRowSelection: (row) => isEntitySelectable(row.original),
    state: {
      sorting,
      columnVisibility,
      columnOrder,
    },
    onColumnOrderChange: () => {},
    onColumnVisibilityChange,
    onSortingChange,
  });
};

export default useTable;

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

import { useMemo, useCallback, useState } from 'react';
import type { ColumnDef, SortingState, Updater, VisibilityState } from '@tanstack/react-table';
import { getCoreRowModel, useReactTable } from '@tanstack/react-table';

import type { EntityBase } from 'components/common/EntityDataTable/types';
import type { Sort } from 'stores/PaginationTypes';
import { UTILITY_COLUMNS } from 'components/common/EntityDataTable/Constants';

type Props<Entity extends EntityBase> = {
  columns: Array<ColumnDef<Entity>>;
  columnOrder: Array<string>;
  entities: ReadonlyArray<Entity>;
  isEntitySelectable: (entity: Entity) => boolean;
  onColumnsChange: (visibleColumns: Array<string>) => void;
  onSortChange: (sort: Sort) => void;
  sort: Sort | undefined;
  visibleColumns: Array<string>;
  initialSelection: any;
  onChangeSelection: (selectedEntities: Array<Entity['id']>, data: Readonly<Array<Entity>>) => void;
};

const useTable = <Entity extends EntityBase>({
  columns,
  columnOrder,
  entities,
  isEntitySelectable,
  onColumnsChange,
  onSortChange,
  sort,
  visibleColumns,
  initialSelection = [],
  onChangeSelection,
}: Props<Entity>) => {
  const [selectedEntities, setSelectedEntities] = useState<Array<Entity['id']>>(initialSelection);

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

  const onRowSelectionChange = useCallback(
    (updater: Updater<Record<string, boolean>>) => {
      const newRowSelection =
        updater instanceof Function ? updater(Object.fromEntries(selectedEntities.map((id) => [id, true]))) : updater;

      const newSelection = Object.entries(newRowSelection)
        .filter(([_id, isSelected]) => isSelected)
        .map(([id]) => id);

      setSelectedEntities(newSelection);

      if (onChangeSelection) {
        onChangeSelection(newSelection, entities);
      }
    },
    [entities, onChangeSelection, selectedEntities],
  );

  return useReactTable({
    columns,
    data,
    enableRowSelection: (row) => isEntitySelectable(row.original),
    enableSortingRemoval: false,
    getCoreRowModel: getCoreRowModel(),
    getRowId: (row) => row.id,
    manualSorting: true,
    onColumnOrderChange: () => {},
    onColumnVisibilityChange,
    onRowSelectionChange,
    onSortingChange,
    state: {
      columnOrder,
      columnVisibility,
      sorting,
      rowSelection: Object.fromEntries(selectedEntities.map((id) => [id, true])),
    },
  });
};

export default useTable;

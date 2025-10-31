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
import { getCoreRowModel, useReactTable, ColumnDef, SortingState, Updater } from '@tanstack/react-table';
import { EntityBase } from 'components/common/EntityDataTable/types';
import type { Sort } from 'stores/PaginationTypes';

type Props<Entity extends EntityBase> = {
  columns: Array<ColumnDef<Entity>>;
  columnsOrder: Array<string>;
  displayBulkSelectCol: boolean;
  entities: ReadonlyArray<Entity>;
  isEntitySelectable: (entity: Entity) => boolean;
  onSortChange: (sort: Sort) => void;
  sort: Sort | undefined;
};

const useTable = <Entity extends EntityBase>({
  columns,
  columnsOrder,
  displayBulkSelectCol,
  entities,
  isEntitySelectable,
  onSortChange,
  sort,
}: Props<Entity>) => {
  const data = useMemo(() => [...entities], [entities]);
  const sorting = useMemo(() => (sort ? [{ id: sort.attributeId, desc: sort.direction === 'desc' }] : []), [sort]);
  // consider adding actions col here. In this case we need to ensure columnsOrder contains all attributes, otherwise missing ocls will be displayed after the actions col.
  const columnOrder = useMemo(
    () => [displayBulkSelectCol ? 'bulk-select' : null, ...columnsOrder].filter(Boolean),
    [displayBulkSelectCol, columnsOrder],
  );
  const onSortingChange = useCallback((updater: Updater<SortingState>) => {
    const newSorting = updater instanceof Function ? updater(sorting) : updater;
    onSortChange({ attributeId: newSorting[0].id, direction: newSorting[0].desc ? 'desc' : 'asc' });
  }, []);

  return useReactTable({
    data,
    columns,
    getCoreRowModel: getCoreRowModel(),
    manualSorting: true,
    enableSortingRemoval: false,
    enableRowSelection: (row) => isEntitySelectable(row.original),
    initialState: {
      columnOrder,
    },
    state: {
      sorting,
    },
    onSortingChange,
  });
};

export default useTable;

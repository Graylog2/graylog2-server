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
import debounceWithPromise from 'views/logic/debounceWithPromise';

import type { ColumnPreferences, EntityBase } from '../types';
import { UTILITY_COLUMNS, ATTRIBUTE_STATUS, ACTIONS_COL_ID } from '../Constants';

const COLUMN_SIZING_PERSIST_DEBOUNCE_IN_MS = 500;

const columnVisibilityChanges = (prevVisibleColumns: VisibilityState, currVisibleColumns: VisibilityState) => {
  const addedColumns = new Set<string>();
  const removedColumns = new Set<string>();

  Object.keys(currVisibleColumns).forEach((key) => {
    if (currVisibleColumns[key] && !prevVisibleColumns[key]) {
      addedColumns.add(key);
    } else if (!currVisibleColumns[key] && prevVisibleColumns[key]) {
      removedColumns.add(key);
    }
  });

  return { addedColumns, removedColumns };
};

const updateColumnPreferences = (
  visibleAttributeColumns: Set<string>,
  removedColumns: Set<string>,
  columnPreferences: ColumnPreferences | undefined = {},
) => {
  const updatedPreferences = { ...columnPreferences };

  // All currently visible columns will be marked as 'show'
  visibleAttributeColumns.forEach((col) => {
    updatedPreferences[col] = { status: ATTRIBUTE_STATUS.show };
  });

  // Only explicitly hidden columns will be marked as 'hide'
  removedColumns.forEach((col) => {
    updatedPreferences[col] = { status: ATTRIBUTE_STATUS.hide };
  });

  return updatedPreferences;
};

type Props<Entity extends EntityBase> = {
  columnOrder: Array<string>;
  columnDefinitions: Array<ColumnDef<Entity>>;
  columnWidths: { [colId: string]: number };
  defaultColumnOrder: Array<string>;
  displayBulkSelectCol: boolean;
  entities: ReadonlyArray<Entity>;
  headerMinWidths: { [colId: string]: number };
  isEntitySelectable: (entity: Entity) => boolean | undefined;
  layoutPreferences: {
    attributes?: ColumnPreferences;
    order?: Array<string>;
  };
  onChangeSelection: (selectedEntities: Array<Entity['id']>, data: Readonly<Array<Entity>>) => void;
  onLayoutPreferencesChange: ({ attributes, order }: { attributes?: ColumnPreferences; order?: Array<string> }) => void;
  onSortChange: (sort: Sort) => void;
  selectedEntities: Array<Entity['id']>;
  setInternalAttributeColumnOrder: (columnOrder: Array<string>) => void;
  setSelectedEntities: (rows: Array<string>) => void;
  setInternalColumnWidthPreferences: React.Dispatch<React.SetStateAction<{ [key: string]: number }>>;
  internalColumnWidthPreferences: { [colId: string]: number };
  sort: Sort | undefined;
};

const useTable = <Entity extends EntityBase>({
  columnOrder,
  columnDefinitions,
  columnWidths,
  defaultColumnOrder,
  displayBulkSelectCol,
  entities,
  headerMinWidths,
  isEntitySelectable = () => true,
  layoutPreferences,
  onChangeSelection,
  onLayoutPreferencesChange,
  onSortChange,
  selectedEntities,
  internalColumnWidthPreferences,
  setInternalColumnWidthPreferences,
  setInternalAttributeColumnOrder,
  setSelectedEntities,
  sort,
}: Props<Entity>) => {
  const data = useMemo(() => [...entities], [entities]);
  const sorting = useMemo(() => (sort ? [{ id: sort.attributeId, desc: sort.direction === 'desc' }] : []), [sort]);
  const debouncedOnLayoutPreferencesChange = useMemo(
    () => debounceWithPromise(onLayoutPreferencesChange, COLUMN_SIZING_PERSIST_DEBOUNCE_IN_MS),
    [onLayoutPreferencesChange],
  );

  const onSortingChange = useCallback(
    (updater: Updater<SortingState>) => {
      const newSorting = updater instanceof Function ? updater(sorting) : updater;
      onSortChange({ attributeId: newSorting[0].id, direction: newSorting[0].desc ? 'desc' : 'asc' });
    },
    [onSortChange, sorting],
  );

  const columnVisibility = useMemo(
    () => Object.fromEntries(columnDefinitions.map(({ id }) => [id, columnOrder.includes(id)])),
    [columnDefinitions, columnOrder],
  );

  const onColumnVisibilityChange = useCallback(
    (updater: Updater<VisibilityState>) => {
      const newColumnVisibility = updater instanceof Function ? updater(columnVisibility) : updater;
      const visibleAttributeColumns = new Set(
        Object.keys(newColumnVisibility).filter((colId) => newColumnVisibility[colId] && !UTILITY_COLUMNS.has(colId)),
      );
      const { addedColumns, removedColumns } = columnVisibilityChanges(columnVisibility, newColumnVisibility);

      const newLayoutPreferences: {
        attributes?: ColumnPreferences;
        order?: Array<string>;
      } = {
        attributes: updateColumnPreferences(visibleAttributeColumns, removedColumns, layoutPreferences.attributes),
      };

      // if user has a custom order, we need to update it to reflect the visibility changes
      if (layoutPreferences.order) {
        const newOrder = layoutPreferences.order.filter((colId) => !removedColumns.has(colId));

        // Insert added columns at their default positions or at the end
        [...addedColumns].forEach((colId) => {
          const defaultIdx = defaultColumnOrder.indexOf(colId);
          if (defaultIdx !== -1 && defaultIdx < newOrder.length) {
            newOrder.splice(defaultIdx, 0, colId);
          } else {
            newOrder.push(colId);
          }
        });

        newLayoutPreferences.order = newOrder;
      }

      return onLayoutPreferencesChange(newLayoutPreferences);
    },
    [
      columnVisibility,
      defaultColumnOrder,
      layoutPreferences.order,
      layoutPreferences.attributes,
      onLayoutPreferencesChange,
    ],
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

  const onColumnOrderChange = useCallback(
    (updater: Updater<Array<string>>) => {
      const newColumnOrder = (updater instanceof Function ? updater(columnOrder) : updater).filter(
        (colId) => !UTILITY_COLUMNS.has(colId),
      );
      setInternalAttributeColumnOrder(newColumnOrder);
      onLayoutPreferencesChange({ order: newColumnOrder });
    },
    [columnOrder, onLayoutPreferencesChange, setInternalAttributeColumnOrder],
  );

  const onColumnSizingChange = useCallback(
    (updater: Updater<{ [colId: string]: number }>) => {
      const newAttributeWidthPreferences =
        updater instanceof Function ? updater(internalColumnWidthPreferences) : updater;

      const clampedAttributeWidths = Object.fromEntries(
        Object.entries(newAttributeWidthPreferences).map(([colId, width]) => [
          colId,
          Math.max(width, headerMinWidths[colId]),
        ]),
      );

      setInternalColumnWidthPreferences(clampedAttributeWidths);

      const newAttributePreferences = { ...(layoutPreferences?.attributes || {}) };

      Object.entries(clampedAttributeWidths).forEach(([colId, width]) => {
        newAttributePreferences[colId] = {
          ...newAttributePreferences[colId],
          width,
        };
      });

      return debouncedOnLayoutPreferencesChange({ attributes: newAttributePreferences });
    },
    [
      debouncedOnLayoutPreferencesChange,
      headerMinWidths,
      internalColumnWidthPreferences,
      layoutPreferences?.attributes,
      setInternalColumnWidthPreferences,
    ],
  );

  // eslint-disable-next-line react-hooks/incompatible-library
  return useReactTable({
    columns: columnDefinitions,
    columnResizeMode: 'onChange',
    data,
    enableRowSelection: (row) => displayBulkSelectCol && isEntitySelectable(row.original),
    enableSortingRemoval: false,
    getCoreRowModel: getCoreRowModel(),
    getRowId: (row) => row.id,
    manualSorting: true,
    onColumnOrderChange,
    onColumnVisibilityChange,
    onRowSelectionChange,
    onSortingChange,
    onColumnSizingChange,
    initialState: {
      columnPinning: {
        right: [ACTIONS_COL_ID],
      },
    },
    state: {
      columnOrder,
      columnVisibility,
      sorting,
      rowSelection,
      columnSizing: columnWidths,
    },
  });
};

export default useTable;

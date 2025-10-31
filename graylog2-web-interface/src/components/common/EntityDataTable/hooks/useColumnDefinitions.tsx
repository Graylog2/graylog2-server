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

import type { EntityBase, ColumnRenderersByAttribute, Column } from 'components/common/EntityDataTable/types';
import * as React from 'react';
import { useMemo } from 'react';
import { createColumnHelper, ColumnDef } from '@tanstack/react-table';
import ButtonToolbar from '../../../bootstrap/ButtonToolbar';
import camelCase from 'lodash/camelCase';
import RowCheckbox from 'components/common/EntityDataTable/RowCheckbox';
import { BULK_SELECT_COLUMN_WIDTH } from 'components/common/EntityDataTable/Constants';
import BulkSelectHead from 'components/common/EntityDataTable/BulkSelectHead';

const useBulkSelectCol = <Entity extends EntityBase>(
  displayBulkSelectCol: boolean,
  isEntitySelectable: (entity: Entity) => boolean,
) => {
  const columnHelper = createColumnHelper<Entity>();

  return useMemo(
    () =>
      displayBulkSelectCol
        ? columnHelper.display({
            id: 'bulk-select',
            size: BULK_SELECT_COLUMN_WIDTH,
            // Todo: check is we can make use of enableRowSelection from useReactTable instead
            header: ({ table }) => <BulkSelectHead data={table.options.data.filter(isEntitySelectable)} />,
            cell: ({ row }) => {
              return (
                <RowCheckbox
                  onChange={row.getToggleSelectedHandler()}
                  title={`${row.getIsSelected() ? 'Deselect' : 'Select'} entity`}
                  checked={row.getIsSelected()}
                  disabled={!row.getCanSelect()}
                  aria-label={row.id}
                />
              );
            },
          })
        : null,
    [displayBulkSelectCol, columnHelper],
  );
};

const useActionsCol = <Entity extends EntityBase>(
  displayActionsCol: boolean,
  actionsColWidth: number,
  entityActions?: (entity: Entity) => React.ReactNode,
) => {
  const columnHelper = createColumnHelper<Entity>();

  return useMemo(
    () =>
      displayActionsCol
        ? columnHelper.display({
            id: 'actions',
            size: actionsColWidth,
            header: 'Actions',
            cell: ({ row }) => <ButtonToolbar>{entityActions(row.original)}</ButtonToolbar>,
          })
        : null,
    [],
  );
};

type Props<Entity extends EntityBase, Meta> = {
  actionsColWidth: number;
  columnRenderersByAttribute: ColumnRenderersByAttribute<Entity, Meta>;
  columns: Array<Column>;
  columnsWidths: { [attributeId: string]: number };
  displayActionsCol: boolean;
  displayBulkSelectCol: boolean;
  entityActions?: (entity: Entity) => React.ReactNode;
  entityAttributesAreCamelCase: boolean;
  isEntitySelectable: (entity: Entity) => boolean;
  meta: Meta;
};
const useColumnDefinitions = <Entity extends EntityBase, Meta>({
  actionsColWidth,
  columnRenderersByAttribute,
  columns,
  columnsWidths,
  displayActionsCol,
  displayBulkSelectCol,
  entityActions,
  entityAttributesAreCamelCase,
  isEntitySelectable,
  meta,
}: Props<Entity, Meta>) => {
  const columnHelper = createColumnHelper<Entity>();
  const bulkSelectCol = useBulkSelectCol(displayBulkSelectCol, isEntitySelectable);
  const actionsCol = useActionsCol(displayActionsCol, actionsColWidth, entityActions);

  const attributeCols = useMemo(
    () =>
      columns.map((col) => {
        const columnRenderer = columnRenderersByAttribute[col.id];
        return columnHelper.accessor(col.id, {
          accessorKey: entityAttributesAreCamelCase ? camelCase(col.id) : col.id,
          cell: ({ cell, row }) => columnRenderer.renderCell(cell.getValue(), row.original, col, meta),
          header: () => columnRenderer?.renderHeader?.(col) ?? col.title,
          size: columnsWidths[col.id],
          enableSorting: col.sortable ?? false,
        });
      }),
    [columns, columnRenderersByAttribute, entityAttributesAreCamelCase, columnsWidths, meta, columnHelper],
  );

  return useMemo(
    () => [bulkSelectCol, ...attributeCols, actionsCol].filter(Boolean) as ColumnDef<Entity, unknown>[],
    [bulkSelectCol, attributeCols, actionsCol],
  );
};

export default useColumnDefinitions;

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
import { useMemo, useCallback } from 'react';
import type { ColumnDef } from '@tanstack/react-table';
import { createColumnHelper } from '@tanstack/react-table';
import camelCase from 'lodash/camelCase';
import styled from 'styled-components';

import type { EntityBase, ColumnRenderersByAttribute, ColumnSchema } from 'components/common/EntityDataTable/types';
import RowCheckbox from 'components/common/EntityDataTable/RowCheckbox';
import {
  BULK_SELECT_COLUMN_WIDTH,
  BULK_SELECT_COL_ID,
  ACTIONS_COL_ID,
} from 'components/common/EntityDataTable/Constants';
import { ButtonToolbar } from 'components/bootstrap';

const useBulkSelectCol = <Entity extends EntityBase>(displayBulkSelectCol: boolean) => {
  const columnHelper = createColumnHelper<Entity>();

  return useMemo(
    () =>
      displayBulkSelectCol
        ? columnHelper.display({
            id: BULK_SELECT_COL_ID,
            size: BULK_SELECT_COLUMN_WIDTH,
            header: ({ table }) => {
              const checked = table.getIsAllRowsSelected();
              const title = `${checked ? 'Deselect' : 'Select'} all visible entities`;

              return (
                <RowCheckbox
                  onChange={table.getToggleAllRowsSelectedHandler()}
                  checked={checked}
                  indeterminate={table.getIsSomeRowsSelected()}
                  title={title}
                  disabled={!table.options?.data?.length}
                  aria-label={title}
                />
              );
            },

            enableHiding: false,
            cell: ({ row }) => (
              <RowCheckbox
                onChange={row.getToggleSelectedHandler()}
                title={`${row.getIsSelected() ? 'Deselect' : 'Select'} entity`}
                checked={row.getIsSelected()}
                disabled={!row.getCanSelect()}
              />
            ),
          })
        : null,
    [displayBulkSelectCol, columnHelper],
  );
};

const ActionsHead = styled.div`
  text-align: right;
`;

const Actions = styled(ButtonToolbar)`
  justify-content: flex-end;
`;

const useActionsCol = <Entity extends EntityBase>(
  displayActionsCol: boolean,
  actionsColWidth: number,
  entityActions: (entity: Entity) => React.ReactNode | undefined,
  colRef: React.MutableRefObject<HTMLDivElement>,
) => {
  const columnHelper = createColumnHelper<Entity>();

  return useMemo(
    () =>
      displayActionsCol
        ? columnHelper.display({
            id: ACTIONS_COL_ID,
            size: actionsColWidth,
            header: () => <ActionsHead>Actions</ActionsHead>,
            enableHiding: false,
            cell: ({ row }) => (
              <div ref={colRef}>
                <Actions>{entityActions(row.original)}</Actions>
              </div>
            ),
          })
        : null,
    [actionsColWidth, colRef, columnHelper, displayActionsCol, entityActions],
  );
};

const useAttributeCols = <Entity extends EntityBase, Meta>({
  columnSchemas,
  columnRenderersByAttribute,
  columnsWidths,
  entityAttributesAreCamelCase,
  meta,
  columnHelper,
}: {
  columnSchemas: Array<ColumnSchema>;
  columnRenderersByAttribute: ColumnRenderersByAttribute<Entity, Meta>;
  columnsWidths: { [attributeId: string]: number };
  entityAttributesAreCamelCase: boolean;
  meta: Meta;
  columnHelper: ReturnType<typeof createColumnHelper<Entity>>;
}) => {
  const cell = useCallback(
    ({ row, getValue, column }) =>
      column.columnDef.meta.columnRenderer?.renderCell?.(getValue(), row.original, meta) ?? getValue(),
    [meta],
  );

  const header = useCallback((ctx) => {
    if (!ctx) {
      return null;
    }
    const columnDefMeta = ctx.column.columnDef.meta;

    return columnDefMeta.renderHeader?.(columnDefMeta.label) ?? columnDefMeta.label;
  }, []);

  return useMemo(
    () =>
      columnSchemas.map((col) => {
        const columnRenderer = columnRenderersByAttribute[col.id];
        const baseColDef = {
          id: col.id,
          cell,
          header,
          size: columnsWidths[col.id],
          enableHiding: true,
          meta: {
            label: col.title,
            columnRenderer,
          },
        };

        if (col.isDerived) {
          return columnHelper.display(baseColDef);
        }

        const attributeName = entityAttributesAreCamelCase ? camelCase(col.id) : col.id;

        return columnHelper.accessor((row) => row[attributeName], {
          enableSorting: col.sortable ?? false,
          ...baseColDef,
        });
      }),
    [
      columnSchemas,
      columnRenderersByAttribute,
      cell,
      header,
      columnsWidths,
      entityAttributesAreCamelCase,
      columnHelper,
    ],
  );
};

type Props<Entity extends EntityBase, Meta> = {
  actionsRef: React.MutableRefObject<HTMLDivElement>;
  actionsColWidth: number;
  columnRenderersByAttribute: ColumnRenderersByAttribute<Entity, Meta>;
  columnSchemas: Array<ColumnSchema>;
  columnsWidths: { [attributeId: string]: number };
  displayActionsCol: boolean;
  displayBulkSelectCol: boolean;
  entityActions?: (entity: Entity) => React.ReactNode;
  entityAttributesAreCamelCase: boolean;
  meta: Meta;
};

const useColumnDefinitions = <Entity extends EntityBase, Meta>({
  actionsRef,
  actionsColWidth,
  columnRenderersByAttribute,
  columnSchemas,
  columnsWidths,
  displayActionsCol,
  displayBulkSelectCol,
  entityActions,
  entityAttributesAreCamelCase,
  meta,
}: Props<Entity, Meta>) => {
  const columnHelper = createColumnHelper<Entity>();
  const bulkSelectCol = useBulkSelectCol(displayBulkSelectCol);
  const actionsCol = useActionsCol(displayActionsCol, actionsColWidth, entityActions, actionsRef);
  const attributeCols = useAttributeCols<Entity, Meta>({
    columnSchemas,
    columnRenderersByAttribute,
    columnsWidths,
    entityAttributesAreCamelCase,
    meta,
    columnHelper,
  });

  return useMemo(
    () => [bulkSelectCol, ...attributeCols, actionsCol].filter(Boolean) as ColumnDef<Entity, unknown>[],
    [bulkSelectCol, attributeCols, actionsCol],
  );
};

export default useColumnDefinitions;

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
import { useMemo } from 'react';
import type { ColumnDef, CellContext } from '@tanstack/react-table';
import { createColumnHelper } from '@tanstack/react-table';
import camelCase from 'lodash/camelCase';
import styled from 'styled-components';

import type { EntityBase, ColumnRenderersByAttribute, Column } from 'components/common/EntityDataTable/types';
import RowCheckbox from 'components/common/EntityDataTable/RowCheckbox';
import {
  BULK_SELECT_COLUMN_WIDTH,
  BULK_SELECT_COL_ID,
  ACTIONS_COL_ID,
} from 'components/common/EntityDataTable/Constants';
import BulkSelectHead from 'components/common/EntityDataTable/BulkSelectHead';
import { ButtonToolbar } from 'components/bootstrap';

const useBulkSelectCol = <Entity extends EntityBase>(
  displayBulkSelectCol: boolean,
  isEntitySelectable: (entity: Entity) => boolean,
) => {
  const columnHelper = createColumnHelper<Entity>();

  return useMemo(
    () =>
      displayBulkSelectCol
        ? columnHelper.display({
            id: BULK_SELECT_COL_ID,
            size: BULK_SELECT_COLUMN_WIDTH,
            // Todo: check if we can make use of enableRowSelection from useReactTable instead
            header: ({ table }) => <BulkSelectHead data={table.options.data.filter(isEntitySelectable)} />,
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
    [displayBulkSelectCol, columnHelper, isEntitySelectable],
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

type Props<Entity extends EntityBase, Meta> = {
  actionsRef: React.MutableRefObject<HTMLDivElement>;
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
  actionsRef,
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
  const actionsCol = useActionsCol(displayActionsCol, actionsColWidth, entityActions, actionsRef);

  const attributeCols = useMemo(
    () =>
      columns.map((col) => {
        const columnRenderer = columnRenderersByAttribute[col.id];
        const baseColDef = {
          id: col.id,
          cell: ({ row, getValue }: CellContext<Entity, unknown>) =>
            columnRenderer?.renderCell?.(getValue(), row.original, col, meta) ?? getValue(),
          header: () => columnRenderer?.renderHeader?.(col) ?? col.title,
          size: columnsWidths[col.id],
          enableHiding: true,
          meta: {
            label: col.title,
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
    [columns, columnRenderersByAttribute, entityAttributesAreCamelCase, columnsWidths, meta, columnHelper],
  );

  return useMemo(
    () => [bulkSelectCol, ...attributeCols, actionsCol].filter(Boolean) as ColumnDef<Entity, unknown>[],
    [bulkSelectCol, attributeCols, actionsCol],
  );
};

export default useColumnDefinitions;

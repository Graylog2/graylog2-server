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

import type { createColumnHelper, Row, Column, HeaderContext, CellContext } from '@tanstack/react-table';
import { useCallback, useMemo } from 'react';
import camelCase from 'lodash/camelCase';

import type {
  EntityBase,
  ColumnRenderersByAttribute,
  ColumnMetaContext,
} from 'components/common/EntityDataTable/types';
import type { ColumnSchema } from 'components/common/EntityDataTable';

const AttributeHeader = <Entity extends EntityBase>(ctx: HeaderContext<Entity, unknown>) => {
  if (!ctx) {
    return null;
  }
  const columnDefMeta = ctx.column.columnDef.meta as ColumnMetaContext<Entity>;

  return columnDefMeta?.columnRenderer?.renderHeader?.(columnDefMeta.label) ?? columnDefMeta.label;
};

const useAttributeColumnDefinitions = <Entity extends EntityBase, Meta>({
  columnSchemas,
  columnRenderersByAttribute,
  columnWidths,
  entityAttributesAreCamelCase,
  meta,
  columnHelper,
}: {
  columnSchemas: Array<ColumnSchema>;
  columnRenderersByAttribute: ColumnRenderersByAttribute<Entity, Meta>;
  columnWidths: { [attributeId: string]: number };
  entityAttributesAreCamelCase: boolean;
  meta: Meta;
  columnHelper: ReturnType<typeof createColumnHelper<Entity>>;
}) => {
  const cell = useCallback(
    ({
      row,
      getValue,
      column,
    }: {
      row: Row<Entity>;
      getValue: CellContext<Entity, unknown>['getValue'];
      column: Column<Entity>;
    }) => {
      const columnDefMeta = column.columnDef.meta as ColumnMetaContext<Entity>;

      return columnDefMeta?.columnRenderer?.renderCell?.(getValue(), row.original, meta) ?? getValue();
    },
    [meta],
  );

  return useMemo(
    () =>
      columnSchemas.map((col) => {
        const baseColDef = {
          id: col.id,
          cell,
          header: AttributeHeader<Entity>,
          size: columnWidths[col.id],
          enableHiding: true,
          enableResizing: !columnRenderersByAttribute[col.id].staticWidth,
          meta: {
            label: col.title,
            columnRenderer: columnRenderersByAttribute[col.id],
            enableColumnOrdering: true,
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
    [columnSchemas, columnRenderersByAttribute, cell, columnWidths, entityAttributesAreCamelCase, columnHelper],
  );
};

export default useAttributeColumnDefinitions;

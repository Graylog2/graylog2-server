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
import { useCallback, useMemo, useContext, useLayoutEffect } from 'react';
import type { createColumnHelper, Row, Column, HeaderContext, CellContext } from '@tanstack/react-table';
import camelCase from 'lodash/camelCase';
import { useSortable } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { styled } from 'styled-components';

import type {
  EntityBase,
  ColumnRenderersByAttribute,
  ColumnMetaContext,
} from 'components/common/EntityDataTable/types';
import type { ColumnSchema } from 'components/common/EntityDataTable';
import DragHandle from 'components/common/SortableList/DragHandle';
import DndStylesContext from 'components/common/EntityDataTable/contexts/DndStylesContext';
import useHeaderSectionObserver from 'components/common/EntityDataTable/hooks/useHeaderSectionObserver';
import ResizeHandle from 'components/common/EntityDataTable/ResizeHandle';

import SortIcon from '../SortIcon';

export const ThInner = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  height: 100%;
`;

export const LeftCol = styled.div`
  display: flex;
  align-items: center;
  height: 100%;
`;

const RightCol = styled.div`
  display: flex;
  align-items: center;
`;

const useSortableCol = (colId: string, disabled: boolean) => {
  const { setColumnTransform } = useContext(DndStylesContext);
  const { attributes, isDragging, listeners, setNodeRef, transform, setActivatorNodeRef } = useSortable({
    id: colId,
    disabled,
  });
  const cssTransform = CSS.Translate.toString(transform);

  useLayoutEffect(() => {
    setColumnTransform((cur) => ({
      ...cur,
      [colId]: cssTransform,
    }));
  }, [colId, setColumnTransform, cssTransform]);

  return {
    attributes,
    isDragging,
    listeners,
    setNodeRef,
    setActivatorNodeRef,
  };
};

const AttributeHeader = <Entity extends EntityBase>({
  ctx,
  onHeaderSectionResize,
}: {
  ctx: HeaderContext<Entity, unknown>;
  onHeaderSectionResize: (colId: string, part: 'left' | 'right', width: number) => void;
}) => {
  const columnMeta = ctx.column.columnDef.meta as ColumnMetaContext<Entity>;
  const { attributes, isDragging, listeners, setNodeRef, setActivatorNodeRef } = useSortableCol(
    ctx.header.column.id,
    !columnMeta?.enableColumnOrdering,
  );
  const leftRef = useHeaderSectionObserver(ctx.header.column.id, 'left', onHeaderSectionResize);
  const rightRef = useHeaderSectionObserver(ctx.header.column.id, 'right', onHeaderSectionResize);

  return (
    <ThInner ref={setNodeRef}>
      <LeftCol ref={leftRef}>
        {columnMeta?.enableColumnOrdering && (
          <DragHandle
            ref={setActivatorNodeRef}
            index={ctx.header.index}
            dragHandleProps={{ ...attributes, ...listeners }}
            isDragging={isDragging}
            itemTitle={columnMeta.label}
          />
        )}
        {columnMeta?.columnRenderer?.renderHeader?.(columnMeta.label) ?? columnMeta.label}
        {ctx.header.column.getCanSort() && <SortIcon<Entity> column={ctx.header.column} />}
      </LeftCol>
      <RightCol ref={rightRef}>
        {ctx.header.column.getCanResize() && (
          <ResizeHandle
            onMouseDown={ctx.header.getResizeHandler()}
            onTouchStart={ctx.header.getResizeHandler()}
            colTitle={columnMeta.label}
          />
        )}
      </RightCol>
    </ThInner>
  );
};

const useAttributeColumnDefinitions = <Entity extends EntityBase, Meta>({
  columnSchemas,
  columnRenderersByAttribute,
  columnWidths,
  entityAttributesAreCamelCase,
  meta,
  columnHelper,
  onHeaderSectionResize,
}: {
  columnSchemas: Array<ColumnSchema>;
  columnRenderersByAttribute: ColumnRenderersByAttribute<Entity, Meta>;
  columnWidths: { [attributeId: string]: number };
  entityAttributesAreCamelCase: boolean;
  meta: Meta;
  columnHelper: ReturnType<typeof createColumnHelper<Entity>>;
  onHeaderSectionResize: (colId: string, part: 'left' | 'right', width: number) => void;
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

  const header = useCallback(
    (ctx) => <AttributeHeader<Entity> ctx={ctx} onHeaderSectionResize={onHeaderSectionResize} />,
    [onHeaderSectionResize],
  );

  return useMemo(
    () =>
      columnSchemas.map((col) => {
        const baseColDef = {
          id: col.id,
          cell,
          header,
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
    [columnSchemas, cell, header, columnWidths, columnRenderersByAttribute, entityAttributesAreCamelCase, columnHelper],
  );
};

export default useAttributeColumnDefinitions;

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
import { styled, css } from 'styled-components';

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
import HeaderActionsDropdown from 'components/common/EntityDataTable/HeaderActionsDropdown';
import Icon from 'components/common/Icon';
import ActiveSliceColContext from 'components/common/EntityDataTable/contexts/ActiveSliceColContext';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';

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

const ActiveSliceIcon = styled(Icon)(
  ({ theme }) => css`
    margin-left: ${theme.spacings.xs};
    color: ${theme.colors.gray[20]};
  `,
);

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
  onChangeSlicing,
  appSection,
}: {
  ctx: HeaderContext<Entity, unknown>;
  onHeaderSectionResize: (colId: string, part: 'left' | 'right', width: number) => void;
  onChangeSlicing: (sliceCol: string | undefined, slice?: string) => void;
  appSection?: string;
}) => {
  const sendTelemetry = useSendTelemetry();
  const activeSliceCol = useContext(ActiveSliceColContext);
  const colId = ctx.header.column.id;
  const columnMeta = ctx.column.columnDef.meta as ColumnMetaContext<Entity>;
  const { attributes, isDragging, listeners, setNodeRef, setActivatorNodeRef } = useSortableCol(
    colId,
    !columnMeta?.enableColumnOrdering,
  );
  const leftRef = useHeaderSectionObserver(colId, 'left', onHeaderSectionResize);
  const rightRef = useHeaderSectionObserver(colId, 'right', onHeaderSectionResize);
  const _onChangeSlicing = () => {
    if (appSection) {
      sendTelemetry(TELEMETRY_EVENT_TYPE.ENTITY_DATA_TABLE.SLICE_COLUMN_SELECTED_HEADER, {
        app_section: appSection,
        app_action_value: 'slice-column-header',
        event_details: { attribute_id: colId },
      });
    }
    onChangeSlicing(colId);
  };
  const _onRemoveSlicing = () => {
    if (appSection) {
      sendTelemetry(TELEMETRY_EVENT_TYPE.ENTITY_DATA_TABLE.SLICE_REMOVED, {
        app_section: appSection,
        app_action_value: 'slice-remove',
        event_details: { attribute_id: colId },
      });
    }
    onChangeSlicing(undefined, undefined);
  };
  const columnLabel = columnMeta?.label ?? colId;
  const headerLabel = columnMeta?.columnRenderer?.renderHeader?.(columnLabel) ?? columnLabel;
  const canSlice = columnMeta?.enableSlicing;
  const isSliceActive = activeSliceCol === colId;
  const canSort = ctx.header.column.getCanSort();
  const sortDirection = ctx.header.column.getIsSorted();

  return (
    <ThInner ref={setNodeRef}>
      <LeftCol ref={leftRef}>
        {columnMeta?.enableColumnOrdering && (
          <DragHandle
            ref={setActivatorNodeRef}
            index={ctx.header.index}
            dragHandleProps={{ ...attributes, ...listeners }}
            isDragging={isDragging}
            itemTitle={columnLabel}
          />
        )}
        <HeaderActionsDropdown
          label={columnLabel}
          activeSort={sortDirection}
          isSliceActive={isSliceActive}
          onChangeSlicing={
            canSlice ? (isSliceActive ? _onRemoveSlicing : _onChangeSlicing) : undefined
          }
          onSort={canSort ? (desc) => ctx.table.setSorting([{ id: colId, desc }]) : undefined}>
          {headerLabel}
        </HeaderActionsDropdown>
        {isSliceActive && <ActiveSliceIcon name="surgical" title={`Slicing by ${columnLabel}`} size="xs" />}
        {sortDirection && <SortIcon<Entity> column={ctx.header.column} />}
      </LeftCol>
      <RightCol ref={rightRef}>
        {ctx.header.column.getCanResize() && (
          <ResizeHandle
            onMouseDown={ctx.header.getResizeHandler()}
            onTouchStart={ctx.header.getResizeHandler()}
            colTitle={columnLabel}
          />
        )}
      </RightCol>
    </ThInner>
  );
};

const useAttributeColumnDefinitions = <Entity extends EntityBase, Meta>({
  columnHelper,
  columnRenderersByAttribute,
  columnSchemas,
  columnWidths,
  entityAttributesAreCamelCase,
  meta,
  onChangeSlicing,
  onHeaderSectionResize,
  appSection,
}: {
  columnHelper: ReturnType<typeof createColumnHelper<Entity>>;
  columnRenderersByAttribute: ColumnRenderersByAttribute<Entity, Meta>;
  columnSchemas: Array<ColumnSchema>;
  columnWidths: { [attributeId: string]: number };
  entityAttributesAreCamelCase: boolean;
  meta: Meta;
  onChangeSlicing: (sliceCol: string | undefined, slice?: string) => void;
  onHeaderSectionResize: (colId: string, part: 'left' | 'right', width: number) => void;
  appSection?: string;
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
    (ctx) => (
      <AttributeHeader<Entity>
        ctx={ctx}
        onHeaderSectionResize={onHeaderSectionResize}
        onChangeSlicing={onChangeSlicing}
        appSection={appSection}
      />
    ),
    [appSection, onChangeSlicing, onHeaderSectionResize],
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
            enableSlicing: col.sliceable,
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

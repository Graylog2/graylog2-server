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
import type { Column } from '@tanstack/react-table';
import { forwardRef, type ForwardedRef, useContext } from 'react';
import styled, { css } from 'styled-components';

import zIndices from 'theme/z-indices';
import Icon from 'components/common/Icon';
import type { EntityBase, ColumnMetaContext } from 'components/common/EntityDataTable/types';
import SortIcon from 'components/common/EntityDataTable/SortIcon';
import ResizeHandle from 'components/common/EntityDataTable/ResizeHandle';
import { CELL_PADDING } from 'components/common/EntityDataTable/Constants';
import { ThInner, LeftCol } from 'components/common/EntityDataTable/hooks/useAttributeColumnDefinitions';
import HeaderActionsDropdown from 'components/common/EntityDataTable/HeaderActionsDropdown';
import ActiveSliceColContext from 'components/common/EntityDataTable/contexts/ActiveSliceColContext';

const CustomDragOverlay = styled.div<{ $minWidth: number }>(
  ({ theme, $minWidth }) => css`
    background-color: ${theme.colors.global.contentBackground};
    z-index: ${zIndices.dropdownMenu};
    padding: ${CELL_PADDING}px;
    width: ${$minWidth}px;
    min-width: fit-content;
    font-weight: bold;
    white-space: nowrap;
    max-width: 300px;
    box-shadow:
      rgb(0 0 0 / 5%) 0 1px 3px 0,
      rgb(0 0 0 / 5%) 0 28px 23px -7px,
      rgb(0 0 0 / 4%) 0 12px 12px -7px;

    border-radius: 3px;
    border: 1px solid ${theme.colors.input.borderFocus};
    display: flex;
    align-items: center;
    line-height: 0;
  `,
);

const DragHandle = styled.div<{ $isDragging: boolean }>(
  ({ $isDragging, theme }) => css`
    display: inline-block;
    cursor: ${$isDragging ? 'grabbing' : 'grab'};
    margin-right: ${theme.spacings.xxs};
  `,
);

const DragIcon = styled(Icon)`
  color: ${({ theme }) => theme.colors.text.secondary};
`;

const ActiveSliceIcon = styled(Icon)(
  ({ theme }) => css`
    display: inline-flex;
    margin-left: ${theme.spacings.xs};
    color: ${theme.colors.text.secondary};
    cursor: default;
  `,
);

const ThGhostInner = <Entity extends EntityBase>(
  { column }: { column: Column<Entity> },
  ref: React.ForwardedRef<HTMLDivElement>,
) => {
  const activeSliceCol = useContext(ActiveSliceColContext);
  const columnMeta = column.columnDef?.meta as ColumnMetaContext<any>;
  const columnLabel = columnMeta?.label ?? column.id;
  const canSlice = columnMeta?.enableSlicing;
  const canSort = column.getCanSort();
  const isSliceActive = Boolean(canSlice && activeSliceCol === column.id);
  const sortDirection = column.getIsSorted();

  return (
    <CustomDragOverlay ref={ref} $minWidth={column.getSize()}>
      <ThInner>
        <LeftCol>
          <DragHandle $isDragging>
            <DragIcon name="drag_indicator" />
          </DragHandle>
          <HeaderActionsDropdown
            label={columnLabel}
            activeSort={sortDirection}
            isSliceActive={isSliceActive}
            onChangeSlicing={canSlice ? () => {} : undefined}
            sliceColumnId={column.id}
            onSort={canSort ? () => {} : undefined}>
            {columnLabel}
          </HeaderActionsDropdown>
          {isSliceActive && <ActiveSliceIcon name="surgical" title={`Slicing by ${columnLabel}`} />}
          {sortDirection && <SortIcon<Entity> column={column} />}
        </LeftCol>
        {column.getCanResize() && <ResizeHandle colTitle={columnLabel} />}
      </ThInner>
    </CustomDragOverlay>
  );
};

const ThDragOverlay = forwardRef(ThGhostInner) as <Entity extends EntityBase>(
  props: { column: Column<Entity> } & { ref?: ForwardedRef<HTMLDivElement> },
) => ReturnType<typeof ThGhostInner>;

export default ThDragOverlay;

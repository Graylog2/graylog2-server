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
import { forwardRef, type ForwardedRef } from 'react';
import styled, { css } from 'styled-components';

import zIndices from 'theme/z-indices';
import Icon from 'components/common/Icon';
import type { EntityBase, ColumnMetaContext } from 'components/common/EntityDataTable/types';
import SortIcon from 'components/common/EntityDataTable/SortIcon';

const CustomDragOverlay = styled.div<{ $minWidth: number }>(
  ({ theme, $minWidth }) => css`
    background-color: ${theme.colors.global.contentBackground};
    z-index: ${zIndices.dropdownMenu};
    padding: ${theme.spacings.xs};
    width: ${$minWidth}px;
    font-weight: bold;
    white-space: nowrap;
    max-width: 300px;
    box-shadow:
      rgba(0, 0, 0, 0.05) 0 1px 3px 0,
      rgba(0, 0, 0, 0.05) 0 28px 23px -7px,
      rgba(0, 0, 0, 0.04) 0 12px 12px -7px;

    border-radius: 3px;
    border: 1px solid ${theme.colors.input.borderFocus};
    display: flex;
    align-items: center;
    line-height: 0;
  `,
);

const DragHandle = styled.div<{ $isDragging: boolean }>(
  ({ $isDragging }) => css`
    display: inline-block;
    cursor: ${$isDragging ? 'grabbing' : 'grab'};
  `,
);

const DragIcon = styled(Icon)`
  color: ${({ theme }) => theme.colors.text.secondary};
`;

const ThGhostInner = <Entity extends EntityBase>(
  { column }: { column: Column<Entity> },
  ref: React.ForwardedRef<HTMLDivElement>,
) => {
  const columnMeta = column.columnDef?.meta as ColumnMetaContext<any>;

  return (
    <CustomDragOverlay ref={ref} $minWidth={column.getSize()}>
      <DragHandle $isDragging>
        <DragIcon name="drag_indicator" />
      </DragHandle>
      {columnMeta.label}
      {column.getCanSort() && <SortIcon<Entity> column={column} />}
    </CustomDragOverlay>
  );
};

const ThDragOverlay = forwardRef(ThGhostInner) as <Entity extends EntityBase>(
  props: { column: Column<Entity> } & { ref?: ForwardedRef<HTMLDivElement> },
) => ReturnType<typeof ThGhostInner>;

export default ThDragOverlay;

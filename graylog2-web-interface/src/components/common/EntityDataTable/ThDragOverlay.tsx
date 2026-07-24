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
import {
  ThInner,
  LeftCol,
  IndicatorCol,
  DragIcon,
} from 'components/common/EntityDataTable/hooks/useAttributeColumnDefinitions';
import HeaderActionsDropdown from 'components/common/EntityDataTable/HeaderActionsDropdown';
import ActiveSliceColContext from 'components/common/EntityDataTable/contexts/ActiveSliceColContext';

const CustomDragOverlay = styled.div<{ $minWidth: number }>(
  ({ theme, $minWidth }) => css`
    background-color: ${theme.colors.global.contentBackground};
    z-index: ${zIndices.dropdownMenu};
    width: ${$minWidth}px;
    min-width: fit-content;
    white-space: nowrap;
    max-width: 300px;
    box-shadow:
      rgb(0 0 0 / 5%) 0 1px 3px 0,
      rgb(0 0 0 / 5%) 0 28px 23px -7px,
      rgb(0 0 0 / 4%) 0 12px 12px -7px;

    border-radius: 3px;
    border: 1px solid ${theme.colors.input.borderFocus};
    display: flex;
    align-items: stretch;
  `,
);

const StyledDragIcon = styled(DragIcon)`
  top: -1px;
`;

const ActiveSliceIcon = styled(Icon)<{ $isRightAligned?: boolean }>(
  ({ theme, $isRightAligned }) => css`
    display: inline-flex;
    ${$isRightAligned
      ? css`
          margin-right: ${theme.spacings.xs};
        `
      : css`
          margin-left: ${theme.spacings.xs};
        `}
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
  const canHideColumn = column.getCanHide();
  const isSliceActive = Boolean(canSlice && activeSliceCol === column.id);
  const sortDirection = column.getIsSorted();
  // Matches AttributeHeader: for right-aligned (numeric) columns, the indicator icons and title
  // are mirrored (indicators first, title last) instead of title-then-indicators, and the caret
  // inside the title button itself moves to the other side of the label too (see textAlign below).
  const textAlign = columnMeta?.columnRenderer?.textAlign;
  const isRightAligned = textAlign === 'right';

  const titleGroup = (
    <LeftCol>
      <HeaderActionsDropdown
        label={columnLabel}
        activeSort={sortDirection}
        isSliceActive={isSliceActive}
        onChangeSlicing={canSlice ? () => {} : undefined}
        sliceColumnId={column.id}
        onSort={canSort ? () => {} : undefined}
        onHideColumn={canHideColumn ? () => {} : undefined}
        textAlign={textAlign}>
        {columnLabel}
      </HeaderActionsDropdown>
    </LeftCol>
  );

  const sliceIndicator = isSliceActive && (
    <ActiveSliceIcon name="surgical" title={`Slicing by ${columnLabel}`} $isRightAligned={isRightAligned} />
  );
  const sortIndicator = sortDirection && <SortIcon<Entity> column={column} />;

  const indicatorIcons = (
    <IndicatorCol>
      {isRightAligned ? (
        <>
          {sortIndicator}
          {sliceIndicator}
        </>
      ) : (
        <>
          {sliceIndicator}
          {sortIndicator}
        </>
      )}
    </IndicatorCol>
  );

  return (
    <CustomDragOverlay ref={ref} $minWidth={column.getSize()}>
      <ThInner>
        <StyledDragIcon name="drag_indicator" size="xs" $isDragging />
        {isRightAligned ? (
          <>
            {indicatorIcons}
            {titleGroup}
          </>
        ) : (
          <>
            {titleGroup}
            {indicatorIcons}
          </>
        )}
      </ThInner>
    </CustomDragOverlay>
  );
};

const ThDragOverlay = forwardRef(ThGhostInner) as <Entity extends EntityBase>(
  props: { column: Column<Entity> } & { ref?: ForwardedRef<HTMLDivElement> },
) => ReturnType<typeof ThGhostInner>;

export default ThDragOverlay;

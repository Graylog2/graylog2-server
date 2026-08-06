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
import React, { useContext } from 'react';
import type { Row, HeaderGroup, ColumnPinningPosition } from '@tanstack/react-table';
import { flexRender } from '@tanstack/react-table';
import styled, { css } from 'styled-components';

import { Table as BaseTable } from 'components/bootstrap';
import EntityTableOverrideRow from 'components/common/EntityDataTable/EntityTableOverrideRow';
import ExpandedSections from 'components/common/EntityDataTable/ExpandedSections';
import {
  ACTIONS_COL_ID,
  CELL_PADDING_VERTICAL,
  CELL_PADDING_HORIZONTAL,
} from 'components/common/EntityDataTable/Constants';
import type {
  EntityBase,
  ExpandedSectionRenderers,
  ColumnMetaContext,
  RowOverride,
} from 'components/common/EntityDataTable/types';
import {
  columnOpacityVar,
  columnTransformVar,
  columnTransition,
  displayScrollRightIndicatorVar,
} from 'components/common/EntityDataTable/CSSVariables';
import ScrollShadow from 'theme/box-shadows/ScrollShadow';

import ExpandedEntitiesSectionsContext from './contexts/ExpandedSectionsContext';
import TableHead from './TableHead';

const StyledTable = styled(BaseTable)`
  table-layout: fixed;
  margin-bottom: 0;
  height: 100%; // required to be able to use height: 100% in td
`;

const Td = styled.td<{
  $colId: string;
  $hideEmptyTailBorder: boolean;
  $hidePadding: boolean;
  $isLastVisibleColumn: boolean;
  $pinningPosition: ColumnPinningPosition;
  $textAlign: string;
}>(
  ({ $colId, $hideEmptyTailBorder, $hidePadding, $isLastVisibleColumn, $pinningPosition, $textAlign, theme }) => css`
    word-break: break-word;
    ${$textAlign &&
    css`
      text-align: ${$textAlign};
    `}
    opacity: var(${columnOpacityVar($colId)}, 1);
    transform: var(${columnTransformVar($colId)}, none);
    transition: var(${columnTransition()}, none);
    height: 100%; // required to be able to use height: 100% in child elements
    && {
      padding: ${CELL_PADDING_VERTICAL}px ${CELL_PADDING_HORIZONTAL}px;
    }

    ${$pinningPosition
      ? css`
          position: sticky;
          ${$pinningPosition === 'left' ? 'left' : 'right'}: 0;

          ${ScrollShadow('left')}
          &::before {
            display: var(${displayScrollRightIndicatorVar}, none);
          }
        `
      : ''}

    ${$hidePadding &&
    css`
      && {
        padding: 0;
      }
    `}

    ${$hideEmptyTailBorder &&
    css`
      &&& {
        border-right: none;
      }
    `}

    ${$isLastVisibleColumn &&
    css`
      && {
        border-right-color: ${theme.colors.table.row.divider};
      }
    `}
  `,
);

const Tr = styled.tr<{ $active: boolean }>(({ theme, $active }) =>
  $active
    ? css`
        &&&:not(:hover) {
          background-color: ${theme.colors.table.row.backgroundStriped};
        }
      `
    : '',
);

type Props<Entity extends EntityBase> = {
  columnWidths: { [colId: string]: number };
  expandedSectionRenderers: ExpandedSectionRenderers<Entity> | undefined;
  rowOverride?: RowOverride<Entity>;
  headerGroups: Array<HeaderGroup<Entity>>;
  rows: Array<Row<Entity>>;
};

const Table = <Entity extends EntityBase>({
  columnWidths,
  expandedSectionRenderers,
  rowOverride = undefined,
  headerGroups,
  rows,
}: Props<Entity>) => {
  const { expandedSections } = useContext(ExpandedEntitiesSectionsContext);

  const isRowExpanded = (rowId: string) => !!expandedSections?.[rowId];

  const isTailColumnEmpty = !columnWidths[ACTIONS_COL_ID];
  const leafHeaders = headerGroups[headerGroups.length - 1]?.headers ?? [];
  const lastVisibleColumnId = isTailColumnEmpty ? leafHeaders[leafHeaders.length - 2]?.column.id : undefined;

  return (
    <StyledTable condensed bordered>
      <TableHead columnWidths={columnWidths} headerGroups={headerGroups} />
      {rows.map((row) => {
        const visibleCells = row.getVisibleCells();
        const visibleCellCount = visibleCells.length;
        const renderCell = (cell) => {
          const columnMeta = cell.column.columnDef.meta as ColumnMetaContext<Entity>;

          return (
            <Td
              key={cell.id}
              $colId={cell.column.id}
              $hideEmptyTailBorder={cell.column.id === ACTIONS_COL_ID && isTailColumnEmpty}
              $isLastVisibleColumn={cell.column.id === lastVisibleColumnId}
              $pinningPosition={cell.column.getIsPinned()}
              $hidePadding={columnMeta?.hideCellPadding}
              $textAlign={columnMeta?.columnRenderer?.textAlign}>
              {flexRender(cell.column.columnDef.cell, cell.getContext())}
            </Td>
          );
        };
        const actionCellIndex = visibleCells.findIndex((cell) => cell.column.id === ACTIONS_COL_ID);
        const defaultRowActionCell = actionCellIndex >= 0 ? renderCell(visibleCells[actionCellIndex]) : undefined;
        const overrideNotice = rowOverride?.(row.original);

        return (
          <tbody key={`table-row-${row.id}`} data-testid={`table-row-${row.id}`}>
            {overrideNotice !== undefined ? (
              <EntityTableOverrideRow
                visibleCellCount={visibleCellCount}
                notice={overrideNotice}
                actionCell={defaultRowActionCell}
              />
            ) : (
              <>
                <Tr $active={isRowExpanded(row.id)}>{visibleCells.map(renderCell)}</Tr>
                <ExpandedSections
                  key={`expanded-sections-${row.id}`}
                  expandedSectionRenderers={expandedSectionRenderers}
                  entity={row.original}
                />
              </>
            )}
          </tbody>
        );
      })}
    </StyledTable>
  );
};

export default React.memo(Table) as typeof Table;

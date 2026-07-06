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
import { ACTIONS_COL_ID, ROW_MIN_HEIGHT, UTILITY_COLUMNS } from 'components/common/EntityDataTable/Constants';
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

const StyledTable = styled(BaseTable)(
  ({ theme }) => css`
    table-layout: fixed;
    margin-bottom: 0;
    height: 100%; /* required to be able to use height: 100% in td */

    tbody > tr.active {
      background-color: ${theme.colors.table.row.backgroundStriped} !important;
    }
  `,
);

const Tr = styled.tr`
  height: ${ROW_MIN_HEIGHT}px; /* standardizes row height, acts as a minimum in table layout */
`;

const Td = styled.td<{
  $colId: string;
  $hidePadding: boolean;
  $pinningPosition: ColumnPinningPosition;
  $showDivider: boolean;
  $textAlign: 'left' | 'center' | 'right' | undefined;
  $wrapContent: boolean;
}>(
  ({ $colId, $hidePadding, $pinningPosition, $showDivider, $textAlign, $wrapContent, theme }) => css`
    opacity: var(${columnOpacityVar($colId)}, 1);
    transform: var(${columnTransformVar($colId)}, none);
    transition: var(${columnTransition()}, none);
    height: 100%; /* required to be able to use height: 100% in child elements */
    text-align: ${$textAlign ?? 'center'};

    && {
      vertical-align: middle; /* center content vertically (horizontal alignment unchanged) */
    }

    ${$showDivider &&
    css`
      border-right: 1px solid ${theme.colors.table.row.divider};
    `}

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

    ${$hidePadding
      ? css`
          && {
            padding: 0;
          }
        `
      : css`
          ${$wrapContent
            ? css`
                overflow-wrap: break-word;
              `
            : css`
                white-space: nowrap;
                overflow: hidden;
                text-overflow: ellipsis;
              `}
        `}
  `,
);

type Props<Entity extends EntityBase> = {
  expandedSectionRenderers: ExpandedSectionRenderers<Entity> | undefined;
  rowOverride?: RowOverride<Entity>;
  headerGroups: Array<HeaderGroup<Entity>>;
  rows: Array<Row<Entity>>;
};

const Table = <Entity extends EntityBase>({
  expandedSectionRenderers,
  rowOverride = undefined,
  headerGroups,
  rows,
}: Props<Entity>) => {
  const { expandedSections } = useContext(ExpandedEntitiesSectionsContext);

  const isRowExpanded = (rowId: string) => !!expandedSections?.[rowId];

  return (
    <StyledTable condensed>
      <TableHead headerGroups={headerGroups} />
      {rows.map((row) => {
        const visibleCells = row.getVisibleCells();
        const visibleCellCount = visibleCells.length;
        const renderCell = (cell, index?: number) => {
          const columnMeta = cell.column.columnDef.meta as ColumnMetaContext<Entity>;
          const nextCell = index === undefined ? undefined : visibleCells[index + 1];
          // Column dividers are only drawn between two attribute columns, so the bulk select
          // and the (possibly empty) actions column don't produce stray lines.
          const showDivider =
            !!nextCell && !UTILITY_COLUMNS.has(cell.column.id) && !UTILITY_COLUMNS.has(nextCell.column.id);

          return (
            <Td
              key={cell.id}
              $colId={cell.column.id}
              $pinningPosition={cell.column.getIsPinned()}
              $hidePadding={columnMeta?.hideCellPadding}
              $showDivider={showDivider}
              $textAlign={columnMeta?.columnRenderer?.textAlign}
              $wrapContent={columnMeta?.columnRenderer?.wrapContent}>
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
                <Tr className={isRowExpanded(row.id) ? 'active' : null}>{visibleCells.map(renderCell)}</Tr>
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

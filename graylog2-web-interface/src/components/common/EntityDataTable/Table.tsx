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
import type { Row, HeaderGroup, ColumnPinningPosition } from '@tanstack/react-table';
import { flexRender } from '@tanstack/react-table';
import styled, { css } from 'styled-components';

import { Table as BaseTable } from 'components/bootstrap';
import ExpandedSections from 'components/common/EntityDataTable/ExpandedSections';
import { CELL_PADDING } from 'components/common/EntityDataTable/Constants';
import type { EntityBase, ExpandedSectionRenderers, ColumnMetaContext } from 'components/common/EntityDataTable/types';
import {
  columnOpacityVar,
  columnTransformVar,
  columnTransition,
  displayScrollRightIndicatorVar,
} from 'components/common/EntityDataTable/CSSVariables';
import ScrollShadow from 'theme/box-shadows/ScrollShadow';

import TableHead from './TableHead';

const StyledTable = styled(BaseTable)(
  ({ theme }) => css`
    table-layout: fixed;
    margin-bottom: 0;
    height: 100%; // required to be able to use height: 100% in td

    thead > tr > th,
    tbody > tr > td {
      padding: ${CELL_PADDING}px;
    }

    && {
      > tbody:nth-of-type(even) > tr {
        background-color: ${theme.colors.table.row.backgroundStriped};
      }

      > tbody:nth-of-type(odd) > tr {
        background-color: ${theme.colors.table.row.background};
      }
    }
  `,
);

const Td = styled.td<{
  $colId: string;
  $hidePadding: boolean;
  $pinningPosition: ColumnPinningPosition;
}>(
  ({ $colId, $hidePadding, $pinningPosition }) => css`
    word-break: break-word;
    opacity: var(${columnOpacityVar($colId)}, 1);
    transform: var(${columnTransformVar($colId)}, none);
    transition: var(${columnTransition()}, none);
    height: 100%; // required to be able to use height: 100% in child elements
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
  `,
);

type Props<Entity extends EntityBase> = {
  expandedSectionRenderers: ExpandedSectionRenderers<Entity> | undefined;
  headerGroups: Array<HeaderGroup<Entity>>;
  rows: Array<Row<Entity>>;
};

const Table = <Entity extends EntityBase>({ expandedSectionRenderers, headerGroups, rows }: Props<Entity>) => (
  <StyledTable striped condensed hover>
    <TableHead headerGroups={headerGroups} />
    {rows.map((row) => (
      <tbody key={`table-row-${row.id}`} data-testid={`table-row-${row.id}`}>
        <tr>
          {row.getVisibleCells().map((cell) => {
            const columnMeta = cell.column.columnDef.meta as ColumnMetaContext<Entity>;

            return (
              <Td
                key={cell.id}
                $colId={cell.column.id}
                $pinningPosition={cell.column.getIsPinned()}
                $hidePadding={columnMeta?.hideCellPadding}>
                {flexRender(cell.column.columnDef.cell, cell.getContext())}
              </Td>
            );
          })}
        </tr>
        <ExpandedSections
          key={`expanded-sections-${row.id}`}
          expandedSectionRenderers={expandedSectionRenderers}
          entity={row.original}
        />
      </tbody>
    ))}
  </StyledTable>
);

export default React.memo(Table) as typeof Table;

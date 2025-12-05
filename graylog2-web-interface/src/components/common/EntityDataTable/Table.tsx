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
import type { Row, HeaderGroup } from '@tanstack/react-table';
import { flexRender } from '@tanstack/react-table';
import styled, { css } from 'styled-components';

import { Table as BaseTable } from 'components/bootstrap';
import ExpandedSections from 'components/common/EntityDataTable/ExpandedSections';
import { CELL_PADDING, ACTIONS_COL_ID } from 'components/common/EntityDataTable/Constants';
import type { EntityBase, ExpandedSectionRenderers } from 'components/common/EntityDataTable/types';
import { columnOpacityVar, columnTransformVar, columnTransition } from 'components/common/EntityDataTable/CSSVariables';

import TableHead from './TableHead';

const StyledTable = styled(BaseTable)(
  ({ theme }) => css`
    table-layout: fixed;

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

const Td = styled.td<{ $colId: string; $hidePadding: boolean }>(
  ({ $colId, $hidePadding }) => css`
    word-break: break-word;
    opacity: var(${columnOpacityVar($colId)}, 1);
    transform: var(${columnTransformVar($colId)}, translate3d(0, 0, 0));
    transition: var(${columnTransition()}, none);
    ${$hidePadding &&
    css`
      && {
        padding: 0;
      }
    `}
  `,
);

type Props<Entity extends EntityBase> = {
  hasRowActions: boolean;
  expandedSectionRenderers: ExpandedSectionRenderers<Entity> | undefined;
  rows: Array<Row<Entity>>;
  headerGroups: Array<HeaderGroup<Entity>>;
  registerHeaderSection: (colId: string, part: 'left' | 'right') => (tableHeader: HTMLDivElement | null) => void;
};

const Table = <Entity extends EntityBase>({
  expandedSectionRenderers,
  rows,
  headerGroups,
  hasRowActions,
  registerHeaderSection,
}: Props<Entity>) => (
  <StyledTable striped condensed hover>
    <TableHead
      headerGroups={headerGroups}
      hasRowActions={hasRowActions}
      registerHeaderSection={registerHeaderSection}
    />
    {rows.map((row) => (
      <tbody key={`table-row-${row.id}`} data-testid={`table-row-${row.id}`}>
        <tr>
          {row.getVisibleCells().map((cell) => (
            <Td
              key={cell.id}
              $colId={cell.column.id}
              $hidePadding={!hasRowActions && cell.column.id === ACTIONS_COL_ID}>
              {flexRender(cell.column.columnDef.cell, cell.getContext())}
            </Td>
          ))}
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

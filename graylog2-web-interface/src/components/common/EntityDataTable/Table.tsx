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
import { CELL_PADDING } from 'components/common/EntityDataTable/Constants';
import type { EntityBase, ExpandedSectionRenderers } from 'components/common/EntityDataTable/types';

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

const Td = styled.td<{ $colId: string }>(
  ({ $colId }) => css`
    word-break: break-word;
    opacity: var(--col-${$colId}-opacity, 1);
    transform: var(--col-${$colId}-transform, 'none');
    transition: width transform 0.2s ease-in-out;
  `,
);

type Props<Entity extends EntityBase> = {
  expandedSectionsRenderers: ExpandedSectionRenderers<Entity> | undefined;
  rows: Array<Row<Entity>>;
  headerGroups: Array<HeaderGroup<Entity>>;
};

const Table = <Entity extends EntityBase>({ expandedSectionsRenderers, rows, headerGroups }: Props<Entity>) => (
  <StyledTable striped condensed hover>
    <TableHead headerGroups={headerGroups} />
    {rows.map((row) => (
      <tbody key={`table-row-${row.id}`} data-testid={`table-row-${row.id}`}>
        <tr>
          {row.getVisibleCells().map((cell) => (
            <Td key={cell.id} $colId={cell.column.id}>
              {flexRender(cell.column.columnDef.cell, cell.getContext())}
            </Td>
          ))}
        </tr>
        <ExpandedSections
          key={`expanded-sections-${row.id}`}
          expandedSectionsRenderers={expandedSectionsRenderers}
          entity={row.original}
        />
      </tbody>
    ))}
  </StyledTable>
);

export default React.memo(Table) as typeof Table;

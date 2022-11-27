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
import styled, { css } from 'styled-components';
import { useMemo } from 'react';
import { merge } from 'lodash';

import SortIcon from 'components/streams/StreamsOverview/SortIcon';

import BulkSelectHead from './BulkSelectHead';
import type { Column, Sort, ColumnRenderer, ColumnRenderers } from './types';

const Th = styled.th<{ $width: number | undefined }>(({ $width }) => css`
  width: ${$width ? `${$width}px` : 'auto'};
`);

const TableHeader = <Entity extends { id: string }>({
  activeSort,
  column,
  columnRenderer,
  onSortChange,
  colWidth,
}: {
  activeSort: Sort,
  column: Column
  columnRenderer: ColumnRenderer<Entity> | undefined
  onSortChange: (newSort: Sort) => void,
  colWidth: number
}) => {
  const content = useMemo(
    () => (typeof columnRenderer?.renderHeader === 'function' ? columnRenderer.renderHeader(column) : column.title),
    [column, columnRenderer],
  );

  return (
    <Th $width={colWidth}>
      {content}

      {column.sortable && (
        <SortIcon onChange={onSortChange}
                  column={column}
                  activeSort={activeSort} />
      )}
    </Th>
  );
};

const ActionsHead = styled.th<{ $width: number | undefined }>(({ $width }) => css`
  text-align: right;
  width: ${$width ? `${$width}px` : 'auto'};
`);

const TableHead = <Entity extends { id: string }>({
  actionsColWidth,
  activeSort,
  columns,
  columnRenderers,
  columnsWidths,
  data,
  displayActionsCol,
  displayBulkSelectCol,
  onSortChange,
  selectedEntities,
  setSelectedEntities,
}: {
  actionsColWidth: number | undefined,
  activeSort: Sort,
  columns: Array<Column>,
  columnsWidths: { [columnId: string]: number },
  columnRenderers: ColumnRenderers<Entity>,
  data: Readonly<Array<Entity>>,
  displayActionsCol: boolean,
  displayBulkSelectCol: boolean,
  onSortChange: (newSort: Sort) => void,
  selectedEntities: Array<string>,
  setSelectedEntities: React.Dispatch<React.SetStateAction<Array<string>>>
}) => (
  <thead>
    <tr>
      {displayBulkSelectCol && (
        <BulkSelectHead data={data}
                        selectedEntities={selectedEntities}
                        setSelectedEntities={setSelectedEntities} />
      )}
      {columns.map((column) => {
        const columnRenderer = columnRenderers[column.id];

        return (
          <TableHeader<Entity> columnRenderer={columnRenderer}
                               column={column}
                               colWidth={columnsWidths[column.id]}
                               onSortChange={onSortChange}
                               activeSort={activeSort}
                               key={column.title} />
        );
      })}
      {displayActionsCol ? <ActionsHead $width={actionsColWidth}>Actions</ActionsHead> : null}
    </tr>
  </thead>
  );

export default TableHead;

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

import BulkActionsHead from './BulkActionsHead';
import type { Column, Sort, ColumnRenderers, ColumnRenderer } from './types';
import DefaultColumnRenderers from './DefaultColumnRenderers';

const Th = styled.th<{ $width: string | undefined, $maxWidth: string| undefined }>(({ $width, $maxWidth }) => css`
  width: ${$width ?? 'auto'};
  max-width: ${$maxWidth ?? 'none'};
`);

const TableHeader = <Entity extends { id: string }>({
  activeSort,
  column,
  columnRenderer,
  onSortChange,
}: {
  activeSort: Sort,
  column: Column
  columnRenderer: ColumnRenderer<Entity>
  onSortChange: (newSort: Sort) => void,
}) => {
  const content = useMemo(
    () => (typeof columnRenderer?.renderHeader === 'function' ? columnRenderer.renderHeader(column) : column.title),
    [column, columnRenderer],
  );

  return (
    <Th $width={columnRenderer?.width} $maxWidth={columnRenderer?.maxWidth}>
      {content}

      {column.sortable && (
        <SortIcon onChange={onSortChange}
                  column={column}
                  activeSort={activeSort} />
      )}
    </Th>
  );
};

const ActionsHead = styled.th`
  text-align: right;
`;

const TableHead = <Entity extends { id: string }>({
  activeSort,
  columns,
  customColumnRenderers,
  data,
  displayActionsCol,
  displayBulkActionsCol,
  onSortChange,
  selectedEntities,
  setSelectedEntities,
}: {
  activeSort: Sort,
  columns: Array<Column>,
  customColumnRenderers: ColumnRenderers<Entity> | undefined,
  data: Array<Entity>
  displayActionsCol: boolean
  displayBulkActionsCol: boolean,
  onSortChange: (newSort: Sort) => void,
  selectedEntities: Array<string>,
  setSelectedEntities: React.Dispatch<React.SetStateAction<Array<string>>>
}) => (
  <thead>
    <tr>
      {displayBulkActionsCol && (
        <BulkActionsHead data={data}
                         selectedEntities={selectedEntities}
                         setSelectedEntities={setSelectedEntities} />
      )}
      {columns.map((column) => {
        const columnRenderer = merge(DefaultColumnRenderers[column.id] ?? {}, customColumnRenderers?.[column.id] ?? {});

        return (
          <TableHeader<Entity> columnRenderer={columnRenderer}
                               column={column}
                               onSortChange={onSortChange}
                               activeSort={activeSort}
                               key={column.title} />
        );
      })}
      {displayActionsCol ? <ActionsHead>Actions</ActionsHead> : null}
    </tr>
  </thead>
  );

export default TableHead;

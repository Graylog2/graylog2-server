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
import styled from 'styled-components';
import { useMemo } from 'react';

import SortIcon from 'components/streams/StreamsOverview/SortIcon';

import BulkActionsHead from './BulkActionsHead';
import type { Attribute, CustomHeaders, Sort } from './types';

const defaultAttributeHeaderRenderer = {};

const TableHeader = ({
  activeSort,
  attribute,
  headerRenderer,
  onSortChange,
}: {
  headerRenderer: {
    renderHeader: (attribute: Attribute) => React.ReactNode,
    textAlign?: string,
    width?: string,
    maxWidth?: string,
  },
  attribute: Attribute
  activeSort: Sort,
  onSortChange: (newSort: Sort) => void,
}) => {
  const content = useMemo(
    () => (headerRenderer ? headerRenderer.renderHeader(attribute) : attribute.title),
    [attribute, headerRenderer],
  );

  return (
    <th style={{ width: headerRenderer?.width, maxWidth: headerRenderer?.maxWidth }}>
      {content}

      {attribute.sortable && (
        <SortIcon onChange={onSortChange}
                  attribute={attribute}
                  activeSort={activeSort} />
      )}
    </th>
  );
};

const ActionsHead = styled.th`
  text-align: right;
`;

const TableHead = <ListItem extends { id: string }>({
  selectedAttributes,
  customHeaders,
  displayActionsCol,
  displayBulkActionsCol,
  onSortChange,
  activeSort,
  rows,
  selectedItemsIds,
  setSelectedItemsIds,
}: {
  selectedAttributes: Array<Attribute>,
  customHeaders: CustomHeaders,
  displayActionsCol: boolean
  displayBulkActionsCol: boolean,
  onSortChange: (newSort: Sort) => void,
  activeSort: Sort,
  rows: Array<ListItem>
  selectedItemsIds: Array<string>,
  setSelectedItemsIds: React.Dispatch<React.SetStateAction<Array<string>>>
}) => {
  return (
    <thead>
      <tr>
        {displayBulkActionsCol && (
          <BulkActionsHead rows={rows}
                           selectedItemsIds={selectedItemsIds}
                           setSelectedItemsIds={setSelectedItemsIds} />
        )}
        {selectedAttributes.map((attribute) => {
          const headerRenderer = customHeaders?.[attribute.id] ?? defaultAttributeHeaderRenderer[attribute.id];

          return (
            <TableHeader headerRenderer={headerRenderer}
                         attribute={attribute}
                         onSortChange={onSortChange}
                         activeSort={activeSort}
                         key={attribute.title} />
          );
        })}
        {displayActionsCol ? <ActionsHead>Actions</ActionsHead> : null}
      </tr>
    </thead>
  );
};

export default TableHead;

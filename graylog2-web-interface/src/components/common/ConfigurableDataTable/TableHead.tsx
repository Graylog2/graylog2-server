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

const defaultAttributeHeaderRenderer = {

};

const StyledSortIcon = styled(Icon)<{ $sortIsActive: boolean }>(({ theme, $sortIsActive }) => css`
  margin-left: 4px;
  cursor: pointer;
  font-size: ${theme.fonts.size.small};
  color: ${$sortIsActive ? theme.colors.gray[20] : theme.colors.gray[70]};
`);

const SortIconWrapper = styled.div`
  display: inline-block;
`;

const SortIcon = ({
  onChange,
  activeSort,
  attribute,
}: {
  onChange: (newSort: Sort) => void,
  attribute: Attribute,
  activeSort: Sort | undefined,
}) => {
  const iconName = activeSort?.order === 'asc' ? 'arrow-down-wide-short' : 'arrow-up-wide-short';
  const attributeSortIsActive = activeSort?.attributeId === attribute.id;
  const shouldSortAsc = !attributeSortIsActive || activeSort.order === 'desc';
  const nextSortName = shouldSortAsc ? 'ascending' : 'descending';
  const title = `Sort ${attribute.title.toLowerCase()} ${nextSortName}`;

  const _onChange = () => {
    if (shouldSortAsc) {
      onChange({ attributeId: attribute.id, order: 'asc' });

      return;
    }

    onChange({ attributeId: attribute.id, order: 'desc' });
  };

  return (
    <SortIconWrapper title={title} onClick={_onChange}>
      <StyledSortIcon name={iconName}
                      $sortIsActive={attributeSortIsActive} />
    </SortIconWrapper>
  );
};

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

const TableHead = ({
  selectedAttributes,
  customHeaders,
  displayActionsCol,
  displayBatchSelectCol,
  onSortChange,
  activeSort,
}: {
  selectedAttributes: Array<Attribute>,
  customHeaders: CustomHeaders,
  displayActionsCol: boolean
  displayBatchSelectCol: boolean,
  onSortChange: (newSort: Sort) => void,
  activeSort: Sort
}) => (
  <thead>
    <tr>
      {displayBatchSelectCol && <td />}
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

export default TableHead;

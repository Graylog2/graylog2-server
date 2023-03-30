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

import type { Column } from 'components/common/EntityDataTable';
import CommonSortIcon from 'components/common/SortIcon';
import type { Sort } from 'stores/PaginationTypes';

const StyledCommonSortIcon = styled(CommonSortIcon)`
  display: inline-block;
  margin-left: 6px;
  padding: 0;
  cursor: pointer;
`;

const SORT_DIRECTIONS = {
  ASC: 'asc',
  DESC: 'desc',
} as const;

const SORT_ORDER_NAMES = {
  asc: 'ascending',
  desc: 'descending',
};

const SortIcon = ({
  onChange,
  activeSort,
  column,
}: {
  onChange: (newSort: Sort) => void,
  column: Column,
  activeSort: Sort | undefined,
}) => {
  const columnSortIsActive = activeSort?.attributeId === column.id;
  const nextSortDirection = !columnSortIsActive || activeSort.direction === SORT_DIRECTIONS.DESC ? SORT_DIRECTIONS.ASC : SORT_DIRECTIONS.DESC;
  const title = `Sort ${column.title.toLowerCase()} ${SORT_ORDER_NAMES[nextSortDirection]}`;

  const _onChange = () => {
    onChange({ attributeId: column.id, direction: nextSortDirection });
  };

  return (
    <StyledCommonSortIcon activeDirection={columnSortIsActive ? activeSort.direction : undefined}
                          onChange={_onChange}
                          title={title}
                          ascId={SORT_DIRECTIONS.ASC}
                          descId={SORT_DIRECTIONS.DESC} />
  );
};

export default SortIcon;

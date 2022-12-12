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

import type { Sort, Column } from 'components/common/EntityDataTable';
import CommonSortIcon from 'components/common/SortIcon';

const StyledCommonSortIcon = styled(CommonSortIcon)`
  display: inline-block;
  margin-left: 6px;
  padding: 0;
  cursor: pointer;
`;

const SORT_ORDERS = {
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
  const columnSortIsActive = activeSort?.columnId === column.id;
  const nextSortOrder = !columnSortIsActive || activeSort.order === SORT_ORDERS.DESC ? SORT_ORDERS.ASC : SORT_ORDERS.DESC;
  const title = `Sort ${column.title.toLowerCase()} ${SORT_ORDER_NAMES[nextSortOrder]}`;

  const _onChange = () => {
    onChange({ columnId: column.id, order: nextSortOrder });
  };

  return (
    <StyledCommonSortIcon activeDirection={columnSortIsActive ? activeSort.order : undefined}
                          onChange={_onChange}
                          title={title}
                          ascId={SORT_ORDERS.ASC}
                          descId={SORT_ORDERS.DESC} />
  );
};

export default SortIcon;

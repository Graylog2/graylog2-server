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
import type { Column } from '@tanstack/react-table';

import CommonSortIcon from 'components/common/SortIcon';
import type { ColumnMetaContext, EntityBase } from 'components/common/EntityDataTable/types';

const StyledCommonSortIcon = styled(CommonSortIcon)(
  ({ theme }) => css`
    display: inline-block;
    margin-left: ${theme.spacings.xs};
    padding: 0;
    cursor: pointer;
  `,
);

const SORT_DIRECTIONS = {
  ASC: 'asc',
  DESC: 'desc',
} as const;

const SORT_ORDER_NAMES = {
  asc: 'ascending',
  desc: 'descending',
};

const SortIcon = <Entity extends EntityBase>({ column }: { column: Column<Entity> }) => {
  const nextSortDirection = column.getNextSortingOrder() || SORT_DIRECTIONS.ASC;
  const columnMeta = column.columnDef.meta as ColumnMetaContext<Entity>;

  return (
    <StyledCommonSortIcon
      activeDirection={column.getIsSorted() || null}
      onChange={() => column.toggleSorting()}
      title={`Sort ${columnMeta.label.toLowerCase()} ${SORT_ORDER_NAMES[nextSortDirection]}`}
      ascId={SORT_DIRECTIONS.ASC}
      descId={SORT_DIRECTIONS.DESC}
    />
  );
};

export default SortIcon;

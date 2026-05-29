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
import React from 'react';
import { Table as MantineTable } from '@mantine/core';
import styled, { css } from 'styled-components';

export const PINNED_CELL_CLASS_NAME = 'table-pinned-cell';
export const PINNED_CELL_STRIPED_CLASS_NAME = 'table-pinned-cell-striped';

export const getPinnedCellClassName = (isPinned: boolean, isStripedRow: boolean) => {
  if (!isPinned) {
    return undefined;
  }

  return isStripedRow ? PINNED_CELL_STRIPED_CLASS_NAME : PINNED_CELL_CLASS_NAME;
};

type Props = React.PropsWithChildren<{
  className?: string;
}>;

const StyledTable = styled(MantineTable)(
  ({ theme }) => css`
    --table-border-color: ${theme.colors.table.row.divider};
    font-size: inherit;

    & th,
    & td {
      padding: 8px;
      vertical-align: top;
      border-top: 1px solid ${theme.colors.table.row.divider};
    }

    & thead > tr > th {
      background-color: ${theme.colors.table.head.background};
      white-space: nowrap;
      vertical-align: bottom;
      border-top: none;
      border-bottom: 1px solid ${theme.colors.table.row.divider};
    }

    & tbody > tr {
      background-color: ${theme.colors.global.contentBackground};
      transition: background-color 150ms ease-in-out;
    }

    @media print {
      & thead > tr > th {
        white-space: break-spaces;
        word-break: break-all;
      }
    }
  `,
);

const Table = ({ children, className }: Props) => <StyledTable className={className}>{children}</StyledTable>;

/** @component */
export default Table;

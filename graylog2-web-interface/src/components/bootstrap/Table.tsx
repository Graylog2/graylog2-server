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
import type { DefaultTheme } from 'styled-components';

export const TABLE_ROW_HOVER_TRANSITION = 'background-color 150ms ease-in-out';
export const TABLE_ROW_PINNED_HOVER_BG_VAR = '--table-row-pinned-hover-bg';
export const flattenTableBackground = (theme: DefaultTheme, color: string) =>
  theme.utils.flattenColorStack([theme.colors.global.contentBackground, color]);
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
  'data-testid'?: string;
  striped?: boolean;
  hover?: boolean;
  condensed?: boolean;
  bordered?: boolean;
  responsive?: boolean;
}>;

type StyledProps = {
  $striped?: boolean;
  $hover?: boolean;
  $condensed?: boolean;
  $bordered?: boolean;
};

const StyledTable = styled(MantineTable)<StyledProps>(
  ({ theme, $striped, $hover, $condensed, $bordered }) => css`
    --table-border-color: ${theme.colors.table.row.divider};
    font-size: inherit;
    line-height: inherit;

    ${$bordered &&
    css`
      border: 1px solid ${theme.colors.table.row.divider};
    `}

    & th,
    & td {
      padding: ${$condensed ? '5px' : '8px'};
      vertical-align: top;
      border-top: 1px solid ${theme.colors.table.row.divider};
      ${$bordered &&
      css`
        border: 1px solid ${theme.colors.table.row.divider};
      `}
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
      transition: ${TABLE_ROW_HOVER_TRANSITION};
    }

    ${$striped &&
    css`
      & tbody:only-of-type > tr:nth-of-type(odd) {
        background-color: ${theme.colors.table.row.backgroundStriped};
      }

      & tbody:not(:only-of-type):nth-of-type(odd) > tr {
        background-color: ${theme.colors.table.row.background};
      }

      & tbody:not(:only-of-type):nth-of-type(even) > tr {
        background-color: ${theme.colors.table.row.backgroundStriped};
      }
    `}

    ${$hover &&
    css`
      & tbody > tr:hover {
        background-color: ${theme.colors.table.row.backgroundHover};
        ${TABLE_ROW_PINNED_HOVER_BG_VAR}: ${flattenTableBackground(theme, theme.colors.table.row.backgroundHover)};
      }
    `}

    ${$striped &&
    $hover &&
    css`
      & tbody:only-of-type > tr:hover,
      & tbody:not(:only-of-type) > tr:hover {
        background-color: ${theme.colors.table.row.backgroundHover};
        ${TABLE_ROW_PINNED_HOVER_BG_VAR}: ${flattenTableBackground(theme, theme.colors.table.row.backgroundHover)};
      }
    `}

    & thead > tr > th.${PINNED_CELL_CLASS_NAME} {
      background-color: ${flattenTableBackground(theme, theme.colors.table.head.background)};
    }

    & tbody > tr > .${PINNED_CELL_CLASS_NAME}, & tfoot > tr > .${PINNED_CELL_CLASS_NAME} {
      background-color: ${flattenTableBackground(theme, theme.colors.table.row.background)};
    }

    & tbody > tr > .${PINNED_CELL_STRIPED_CLASS_NAME} {
      background-color: ${flattenTableBackground(theme, theme.colors.table.row.backgroundStriped)};
    }

    @media print {
      & thead > tr > th {
        white-space: break-spaces;
        word-break: break-all;
      }
    }
  `,
);

const Table = ({
  children = undefined,
  className = undefined,
  'data-testid': dataTestId = undefined,
  striped = true,
  hover = true,
  condensed = undefined,
  bordered = undefined,
  responsive = undefined,
}: Props) => {
  const table = (
    <StyledTable
      className={className}
      data-testid={dataTestId}
      $striped={striped}
      $hover={hover}
      $condensed={condensed}
      $bordered={bordered}>
      {children}
    </StyledTable>
  );

  if (responsive) {
    return <div style={{ overflowX: 'auto' }}>{table}</div>;
  }

  return table;
};

/** @component */
export default Table;

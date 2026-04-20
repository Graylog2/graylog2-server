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
// eslint-disable-next-line no-restricted-imports
import { Table as BootstrapTable } from 'react-bootstrap';
import styled, { css } from 'styled-components';

const variantRowStyles = css(({ theme }) => {
  const { table } = theme.colors;
  let styles = '';

  const tableVariant = table.variant as Record<string, string>;
  const tableVariantHover = table.variantHover as Record<string, string>;

  const variants: Record<string, { background: string; hover: string }> = {
    active: {
      background: tableVariant.active,
      hover: tableVariantHover.active,
    },
    success: {
      background: tableVariant.success,
      hover: tableVariantHover.success,
    },
    info: {
      background: tableVariant.info,
      hover: tableVariantHover.info,
    },
    warning: {
      background: tableVariant.warning,
      hover: tableVariantHover.warning,
    },
    danger: {
      background: tableVariant.danger,
      hover: tableVariantHover.danger,
    },
  };

  Object.keys(variants).forEach((variant) => {
    const { background, hover } = variants[variant];

    styles += `
      &.table > thead > tr,
      &.table > tfoot > tr,
      &.table > tbody > tr {
        > td.${variant},
        > th.${variant},
        &.${variant} > td,
        &.${variant} > th {
          background-color: ${background};
        }
      }

      &.table-hover > tbody > tr {
        > td.${variant}:hover,
        > th.${variant}:hover,
        &.${variant}:hover > td,
        &:hover > .${variant},
        &.${variant}:hover > th {
          background-color: ${hover};
        }
      }
    `;
  });

  return css`
    ${styles}
  `;
});

const tableCss = css(
  ({ theme }) => css`
    &.table {
      > thead > tr,
      > tbody > tr,
      > tfoot > tr {
        > th,
        > td {
          border-top-color: ${theme.colors.table.row.divider};
          border-width: 1px;
        }
      }

      > thead > tr > th {
        background: ${theme.colors.table.head.background};
        white-space: nowrap;
        border-bottom-color: ${theme.colors.table.row.divider};
        border-width: 1px;
      }

      > tbody > tr {
        background-color: ${theme.colors.global.contentBackground};
        transition: background-color 150ms ease-in-out;
      }

      > tbody + tbody {
        border-top-color: ${theme.colors.table.row.divider};
        border-width: 1px;
      }

      .table {
        background-color: ${theme.colors.table.row.background};
      }
    }

    &.table-bordered {
      border-color: ${theme.colors.table.row.divider};

      > thead > tr,
      > tfoot > tr,
      > tbody > tr {
        > td,
        > th {
          border-color: ${theme.colors.table.row.divider};
        }
      }
    }

    &.table-striped > tbody > tr:nth-of-type(odd) {
      background-color: ${theme.colors.table.row.backgroundStriped};
    }

    &.table-hover > tbody > tr:hover {
      background-color: ${theme.colors.table.row.backgroundHover};
    }

    ${variantRowStyles}

    @media print {
      &.table > thead > tr > th {
        white-space: break-spaces;
        word-break: break-all;
      }
    }
  `,
);

const Table = styled(BootstrapTable)`
  ${tableCss}
`;

/** @component */
export default Table;

export { tableCss };

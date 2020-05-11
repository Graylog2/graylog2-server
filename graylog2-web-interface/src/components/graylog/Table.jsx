// eslint-disable-next-line no-restricted-imports
import { Table as BootstrapTable } from 'react-bootstrap';
import styled, { css } from 'styled-components';

const variantRowStyles = css(({ theme }) => {
  let styles = '';

  const variants = {
    active: {
      background: theme.util.colorLevel(theme.color.global.tableBackgroundAlt, -10),
      hover: theme.util.colorLevel(theme.color.global.tableBackgroundAlt, -9),
    },
    success: {
      background: theme.util.colorLevel(theme.color.variant.success, -10),
      hover: theme.util.colorLevel(theme.color.variant.success, -9),
    },
    info: {
      background: theme.util.colorLevel(theme.color.variant.info, -10),
      hover: theme.util.colorLevel(theme.color.variant.info, -9),
    },
    warning: {
      background: theme.util.colorLevel(theme.color.variant.warning, -10),
      hover: theme.util.colorLevel(theme.color.variant.warning, -9),
    },
    danger: {
      background: theme.util.colorLevel(theme.color.variant.danger, -10),
      hover: theme.util.colorLevel(theme.color.variant.danger, -9),
    },
  };

  Object.keys(variants).forEach((variant) => {
    const { background, hover } = variants[variant];

    styles += `
      &.table > thead > tr > td.${variant},
      &.table > tbody > tr > td.${variant},
      &.table > tfoot > tr > td.${variant},
      &.table > thead > tr > th.${variant},
      &.table > tbody > tr > th.${variant},
      &.table > tfoot > tr > th.${variant},
      &.table > thead > tr.${variant} > td,
      &.table > tbody > tr.${variant} > td,
      &.table > tfoot > tr.${variant} > td,
      &.table > thead > tr.${variant} > th,
      &.table > tbody > tr.${variant} > th,
      &.table > tfoot > tr.${variant} > th {
        background-color: ${background};
      }

      &.table-hover > tbody > tr > td.${variant}:hover,
      &.table-hover > tbody > tr > th.${variant}:hover,
      &.table-hover > tbody > tr.${variant}:hover > td,
      &.table-hover > tbody > tr:hover > .${variant},
      &.table-hover > tbody > tr.${variant}:hover > th {
        background-color: ${hover};
      }
    `;
  });

  return css`
    ${styles}
  `;
});

const Table = styled(BootstrapTable)(({ theme }) => {
  return css`
    background-color: ${theme.color.global.tableBackground};

    &.table > thead > tr > th,
    &.table > tbody > tr > th,
    &.table > tfoot > tr > th,
    &.table > thead > tr > td,
    &.table > tbody > tr > td,
    &.table > tfoot > tr > td {
      border-top-color: ${theme.color.global.tableBackgroundAlt};
    }

    &.table > thead > tr > th {
      border-bottom-color: ${theme.color.global.tableBackgroundAlt};
    }

    &.table > tbody + tbody {
      border-top-color: ${theme.color.global.tableBackgroundAlt};
    }

    .table .table {
      background-color: ${theme.color.gray[100]};
    }

    &.table-bordered {
      border-color: ${theme.color.global.tableBackgroundAlt};
    }

    &.table-bordered > thead > tr > th,
    &.table-bordered > tbody > tr > th,
    &.table-bordered > tfoot > tr > th,
    &.table-bordered > thead > tr > td,
    &.table-bordered > tbody > tr > td,
    &.table-bordered > tfoot > tr > td {
      border-color: ${theme.color.global.tableBackgroundAlt};
    }

    &.table-striped > tbody > tr:nth-of-type(odd) {
      background-color: ${theme.color.gray[90]};
    }

    &.table-hover > tbody > tr:hover {
      background-color: ${theme.color.global.tableBackgroundAlt};
    }

    ${variantRowStyles};
  `;
});

/** @component */
export default Table;

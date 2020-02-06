import styled, { css } from 'styled-components';
// eslint-disable-next-line no-restricted-imports
import { Table as BootstrapTable } from 'react-bootstrap';

import { teinte, util } from 'theme';

const variants = {
  active: {
    background: util.colorLevel(teinte.secondary.tre, -10),
    hover: util.colorLevel(teinte.secondary.tre, -9),
  },
  success: {
    background: util.colorLevel(teinte.tertiary.tre, -10),
    hover: util.colorLevel(teinte.tertiary.tre, -9),
  },
  info: {
    background: util.colorLevel(teinte.tertiary.uno, -10),
    hover: util.colorLevel(teinte.tertiary.uno, -9),
  },
  warning: {
    background: util.colorLevel(teinte.tertiary.sei, -10),
    hover: util.colorLevel(teinte.tertiary.sei, -9),
  },
  danger: {
    background: util.colorLevel(teinte.tertiary.quattro, -10),
    hover: util.colorLevel(teinte.tertiary.quattro, -9),
  },
};

const variantRowStyles = () => {
  let styles = '';
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
};

const Table = styled(BootstrapTable)`
  background-color: transparent;

  &.table > thead > tr > th,
  &.table > tbody > tr > th,
  &.table > tfoot > tr > th,
  &.table > thead > tr > td,
  &.table > tbody > tr > td,
  &.table > tfoot > tr > td {
    border-top-color: ${teinte.secondary.tre};
  }

  &.table > thead > tr > th {
    border-bottom-color: ${teinte.secondary.tre};
  }

  &.table > tbody + tbody {
    border-top-color: ${teinte.secondary.tre};
  }

  .table .table {
    background-color: ${teinte.primary.due};
  }

  &.table-bordered {
    border-color: ${util.colorLevel(teinte.secondary.tre, -2)};
  }

  &.table-bordered > thead > tr > th,
  &.table-bordered > tbody > tr > th,
  &.table-bordered > tfoot > tr > th,
  &.table-bordered > thead > tr > td,
  &.table-bordered > tbody > tr > td,
  &.table-bordered > tfoot > tr > td {
    border-color: ${util.colorLevel(teinte.secondary.tre, -2)};
  }

  &.table-striped > tbody > tr:nth-of-type(odd) {
    background-color: ${util.colorLevel(teinte.secondary.tre, -10)};
  }

  &.table-hover > tbody > tr:hover {
    background-color: ${util.colorLevel(teinte.secondary.tre, -9)};
  }

  ${variantRowStyles()}
`;

export default Table;

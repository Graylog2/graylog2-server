// import React from 'react';
// eslint-disable-next-line no-restricted-imports
import { Row as BootstrapRow } from 'react-bootstrap';
import styled, { css } from 'styled-components';

const Row = styled(BootstrapRow)(({ theme }) => css`
  &.content {
    background-color: ${theme.colors.global.contentBackground};
    border: 1px solid ${theme.colors.gray[80]};
    margin-bottom: 9px;
  }
`);

/** @component */
export default Row;

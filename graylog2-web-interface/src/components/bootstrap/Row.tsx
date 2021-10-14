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
import { Row as BootstrapRow } from 'react-bootstrap';
import styled, { css } from 'styled-components';
import PropTypes from 'prop-types';
import * as React from 'react';

export const RowContentStyles = css(({ theme }) => css`
  background-color: ${theme.colors.global.contentBackground};
  border: 1px solid ${theme.colors.variant.lighter.default};
  margin-bottom: ${theme.spacings.xs};
  border-radius: 4px;
`);

const StyledRow = styled(BootstrapRow)`
  &.content {
    ${RowContentStyles}
  }
`;

const Row = ({ children, ...restProps }) => {
  return <StyledRow {...restProps}>{children}</StyledRow>;
};

Row.propTypes = {
  children: PropTypes.node,
};

Row.defaultProps = {
  children: undefined,
};

export default Row;

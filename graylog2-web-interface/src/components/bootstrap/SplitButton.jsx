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
import PropTypes from 'prop-types';
import * as React from 'react';
// eslint-disable-next-line no-restricted-imports
import { SplitButton as BootstrapSplitButton } from 'react-bootstrap';
import styled, { css } from 'styled-components';

import menuItemStyles from './styles/menuItem';

const StyledSplitButton = styled(BootstrapSplitButton)(({ theme }) => css`
  ${theme.components.button}
  ~ .btn.dropdown-toggle {
    ${theme.components.button}
    & ~ {
      ${menuItemStyles}
    }
  }
`);

const SplitButton = ({ children, ...restProps }) => {
  return <StyledSplitButton {...restProps}>{children}</StyledSplitButton>;
};

SplitButton.propTypes = {
  children: PropTypes.node,
};

SplitButton.defaultProps = {
  children: undefined,
};

export default SplitButton;

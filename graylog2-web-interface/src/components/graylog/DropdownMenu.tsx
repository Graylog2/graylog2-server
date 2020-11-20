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
// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';
import type { StyledComponent } from 'styled-components';

import type { ThemeInterface } from 'theme';

type Props = {
  children: React.Node,
  show: boolean,
};

const StyledDropdownMenu: StyledComponent<{show: boolean}, ThemeInterface, HTMLUListElement> = styled.ul.attrs(() => ({
  className: 'dropdown-menu', /* stylelint-disable-line property-no-unknown */
}))(({ show, theme }) => css`
  display: ${show ? 'block' : 'none'};
  min-width: max-content;
  color: ${theme.colors.variant.dark.default};
  background-color: ${theme.colors.variant.lightest.default};
  box-shadow: 0 3px 3px ${theme.colors.global.navigationBoxShadow};
  padding: 5px;
  z-index: 1050;
  
  .dropdown-header {
    color: ${theme.colors.variant.dark.default};
  }
  
  > li {
    > a {
      color: ${theme.colors.variant.darker.default};
      
      &:hover {
        color: ${theme.colors.variant.darkest.default};
        background-color: ${theme.colors.variant.lighter.default};
      }
    }
    
    &.disabled {
      > a {
        color: ${theme.colors.variant.light.default};
      }
    }
  }
`);

const DropdownMenu = ({ show, children, ...restProps }: Props) => {
  return (
    <StyledDropdownMenu {...restProps} show={show}>
      {children}
    </StyledDropdownMenu>
  );
};

DropdownMenu.propTypes = {
  children: PropTypes.node.isRequired,
  show: PropTypes.bool,
};

DropdownMenu.defaultProps = {
  show: false,
};

export default DropdownMenu;

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
  className: 'dropdown-menu',
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
      color: ${theme.colors.variant.light.info};
      
      &:hover {
        color: ${theme.colors.variant.dark.info};
        background-color: ${theme.colors.variant.lighter.default};
      }
    }
    
    &.disabled {
      > a {
        color: ${theme.colors.variant.lighter.info};
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
  show: PropTypes.boolean,
};

DropdownMenu.defaultProps = {
  show: false,
};

export default DropdownMenu;

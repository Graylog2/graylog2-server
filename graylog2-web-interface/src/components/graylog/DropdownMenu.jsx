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

// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import styled, { type StyledComponent } from 'styled-components';

import { type ThemeInterface } from 'theme/types';
import { Dropdown } from './bootstrap-import';

type Props = {
  children: React.Node,
  left: boolean,
  title: string,
};

const Toggle: StyledComponent<{}, ThemeInterface, HTMLAnchorElement> = styled.a.attrs({
  href: '#',
})(({ theme }) => `
  &::after {
    display: block;
    content: " ";
    float: right;
    width: 0;
    height: 0;
    border-color: transparent;
    border-style: solid;
    border-width: 5px 0 5px 5px;
    border-left-color: ${theme.colors.gray[80]};
    margin-top: 5px;
    margin-right: -10px;
  }
`);

const StyledSubmenu: StyledComponent<{left: boolean}, ThemeInterface, HTMLLIElement> = styled(Dropdown)(({ left, theme }) => `
  position: relative;

  > .dropdown-menu {
    top: 0;
    left: ${left ? 'auto' : '100%'};
    right: ${left ? '98%' : 'auto'};
    margin-top: -6px;
    margin-left: ${left ? '10px' : '-1px'};
    border-radius: ${left ? '6px 0 6px 6px' : '0 6px 6px 6px'};
  }

  &:hover > .dropdown-menu {
    display: block;
  }

  &:hover > ${/* sc-selector */String(Toggle)}::after {
    border-left-color: ${theme.colors.gray[100]};
  }
`);

const DropdownSubmenu = ({ children, left, title }: Props) => {
  return (
    <StyledSubmenu left={left} as="li">
      {title && <Toggle>{title}</Toggle>}

      <Dropdown.Menu>
        {children}
      </Dropdown.Menu>
    </StyledSubmenu>
  );
};

DropdownSubmenu.propTypes = {
  children: PropTypes.node.isRequired,
  left: PropTypes.bool,
  title: PropTypes.string,
};

DropdownSubmenu.defaultProps = {
  left: false,
  title: undefined,
};

export default DropdownSubmenu;

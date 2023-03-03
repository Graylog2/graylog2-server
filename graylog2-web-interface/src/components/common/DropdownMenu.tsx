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
import * as React from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';
import type { SyntheticEvent } from 'react';

type Props = {
  children: React.ReactNode,
  zIndex?: number,
  show: boolean,
  onMenuItemSelect?: (e: SyntheticEvent) => void,
};

const StyledDropdownMenu = styled.ul.attrs(() => ({
  className: 'dropdown-menu' /* stylelint-disable-line property-no-unknown*/
}))<{ $show: boolean, $zIndex: number }>(({ $show, theme, $zIndex }) => css`
  display: ${$show ? 'block' : 'none'};
  min-width: max-content;
  color: ${theme.colors.variant.dark.default};
  background-color: ${theme.colors.variant.lightest.default};
  box-shadow: 0 3px 3px ${theme.colors.global.navigationBoxShadow};
  padding: 5px;
  z-index: ${$zIndex};
  
  .dropdown-header {
    color: ${theme.colors.variant.dark.default};
  }
  
  > li {
    > a {
      color: ${theme.colors.variant.darker.default};
      display: flex;
      align-items: center;
      
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

function closeOnChildSelect(child: React.ReactElement, updateDepth: number, onMenuItemSelect) {
  if (child.props?.onSelect) {
    return {
      onSelect: (_eventKey: string | null | undefined, event: SyntheticEvent<HTMLButtonElement>) => {
        child.props.onSelect();
        onMenuItemSelect(event);
      },
    };
  }

  if (child.props?.children) {
    return {
      // eslint-disable-next-line @typescript-eslint/no-use-before-define
      children: closeOnChildrenSelect(child.props.children, updateDepth + 1, onMenuItemSelect),
    };
  }

  return {};
}

function closeOnChildrenSelect(children: React.ReactNode, updateDepth: number, onToggle) {
  const maxChildDepth = 2;

  if (updateDepth > maxChildDepth) {
    return children;
  }

  return React.Children.map(
    children,
    (child: React.ReactElement) => (child?.props ? React.cloneElement(child, {
      ...child.props,
      ...closeOnChildSelect(child, updateDepth + 1, onToggle),
    }) : child),
  );
}

const DropdownMenu = ({ show, children, zIndex, onMenuItemSelect, ...restProps }: Props) => {
  const mappedChildren = closeOnChildrenSelect(children, 0, onMenuItemSelect);

  return (
    <StyledDropdownMenu {...restProps} $show={show} $zIndex={zIndex}>
      {mappedChildren}
    </StyledDropdownMenu>
  );
};

DropdownMenu.propTypes = {
  children: PropTypes.node.isRequired,
  zIndex: PropTypes.number,
  show: PropTypes.bool,
};

DropdownMenu.defaultProps = {
  show: false,
  zIndex: 1050,
  onMenuItemSelect: () => {},
};

export default DropdownMenu;

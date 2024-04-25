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
// eslint-disable-next-line no-restricted-imports
import { NavDropdown as BootstrapNavDropdown } from 'react-bootstrap';
import styled, { css } from 'styled-components';
import PropTypes from 'prop-types';

import Menu from 'components/bootstrap/Menu';
import { NAV_ITEM_HEIGHT } from 'theme/constants';
import NavItemStateIndicator, {
  hoverIndicatorStyles,
  activeIndicatorStyles,
} from 'components/common/NavItemStateIndicator';

import menuItemStyles from './styles/menuItem';

class ModifiedBootstrapNavDropdown extends BootstrapNavDropdown {
  // eslint-disable-next-line class-methods-use-this
  isActive({ props }, activeKey, activeHref) {
    // NOTE: had to override library as it doesn't respect setting `active={false}`
    if (props.active === false) {
      return false;
    }

    if (
      props.active
      || (activeKey != null && props.eventKey === activeKey)
      || (activeHref && props.href === activeHref)
    ) {
      return true;
    }

    return props.active;
  }
}

const StyledMenuDropdown = styled(Menu.Dropdown)`
  z-index: 1032 !important;
`;

const DropdownTrigger = styled.button<{ $active: boolean }>(({ theme, $active }) => css`
  background: transparent;
  border: 0;
  padding: 0 15px;
  min-height: ${NAV_ITEM_HEIGHT};

  &:hover, &:focus {
    ${hoverIndicatorStyles(theme)}
  }

  ${$active ? activeIndicatorStyles(theme) : ''}


  &:hover,
  &:focus {
    color: ${theme.colors.variant.darker.default};
    background-color: transparent;
  }
`);

const NavItem = styled.span`
  display: inline-flex;
  align-items: center;
  min-height: ${NAV_ITEM_HEIGHT};
  padding: 0;

  @media (width <= 991px) {
    width: 100%;
  }
`;

type Props = {
  title?: React.ReactNode,
  inactiveTitle?: string,
  badge?: React.ComponentType<{ text: React.ReactNode }>,
  noCaret?: boolean,
  hoverTitle?: string,
}

const NavDropdown = ({ title, inactiveTitle, badge: Badge, noCaret, children, hoverTitle }: React.PropsWithChildren<Props>) => {
  const isActive = inactiveTitle ? inactiveTitle !== title : undefined;

  return (
    <Menu>
      <NavItem>
        <Menu.Target>
          <DropdownTrigger $active={isActive} title={hoverTitle}>
            <NavItemStateIndicator>
              {Badge ? <Badge text={title} /> : title}
            </NavItemStateIndicator>
            {' '}
            {noCaret ? null : <span className="caret" />}
          </DropdownTrigger>
        </Menu.Target>
      </NavItem>
      <StyledMenuDropdown>
        {children}
      </StyledMenuDropdown>
    </Menu>
  );
};

NavDropdown.propTypes = {
  title: PropTypes.oneOfType([PropTypes.string, PropTypes.object]).isRequired,
  inactiveTitle: PropTypes.string,
  badge: PropTypes.func,
  hoverTitle: PropTypes.string,
};

NavDropdown.defaultProps = {
  inactiveTitle: undefined,
  badge: undefined,
  noCaret: false,
  hoverTitle: undefined,
};

const ModifiedNavDropdown = styled(ModifiedBootstrapNavDropdown)`
  ${menuItemStyles}
`;

/** @component */
export default NavDropdown;
export { ModifiedNavDropdown };

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
import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { NavItem } from 'components/bootstrap';
import { itemStateIndicatorSelector } from 'components/common/NavItemStateIndicator';

const StyledInactiveNav = styled(NavItem)`
  .navbar-default .navbar-nav > & > a {
    &:hover,
    &:focus {
      background: transparent;
    }
  }
  ${itemStateIndicatorSelector} {
    display: none;
  }
`;

// Don't pass active prop, since NavItem should always be inactive
type Props = {
  active?: boolean,
  className?: string,
}

const InactiveNavItem = ({ active: _active, className, children, ...props }: React.PropsWithChildren<Props>) => (
  <StyledInactiveNav className={className} {...props}>{children}</StyledInactiveNav>
);

InactiveNavItem.propTypes = {
  active: PropTypes.any,
  children: PropTypes.oneOfType([PropTypes.array, PropTypes.element, PropTypes.string]).isRequired,
  className: PropTypes.string,
};

InactiveNavItem.defaultProps = {
  active: undefined,
  className: undefined,
};

export default InactiveNavItem;

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

import { NavItem } from 'components/graylog';

const StyledInactiveNav = styled(NavItem)`
  .navbar-default .navbar-nav > & > a {
    &:hover,
    &:focus {
      background: transparent;
    }
  }
`;

// Don't pass active prop, since NavItem should always be inactive
// eslint-disable-next-line no-unused-vars
function InactiveNavItem({ active, className, children, ...props }) {
  return <StyledInactiveNav className={className} {...props}>{children}</StyledInactiveNav>;
}

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

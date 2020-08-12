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

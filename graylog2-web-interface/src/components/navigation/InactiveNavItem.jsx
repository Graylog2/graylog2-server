import React from 'react';
import PropTypes from 'prop-types';
import { NavItem } from 'react-bootstrap';

// Don't pass active prop, since NavItem should always be inactive
// eslint-disable-next-line no-unused-vars
function InactiveNavItem({ active, children, ...props }) {
  return <NavItem {...props}>{children}</NavItem>;
}

InactiveNavItem.propTypes = {
  active: PropTypes.any,
  children: PropTypes.oneOfType([
    PropTypes.array,
    PropTypes.element,
    PropTypes.string,
  ]).isRequired,
};

InactiveNavItem.defaultProps = {
  active: undefined,
};

export default InactiveNavItem;

import React from 'react';
import PropTypes from 'prop-types';
import { NavItem } from 'components/graylog';

// Don't pass active prop, since NavItem should always be inactive
// eslint-disable-next-line no-unused-vars
function InactiveNavItem({ active, className, children, ...props }) {
  return <NavItem className={className} {...props}>{children}</NavItem>;
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

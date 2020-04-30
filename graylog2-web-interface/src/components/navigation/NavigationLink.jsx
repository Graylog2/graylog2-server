import React from 'react';
import PropTypes from 'prop-types';
import { MenuItem, NavItem } from 'components/graylog';
import { LinkContainer } from 'react-router-bootstrap';

// We render a NavItem if topLevel is set to avoid errors when the NavigationLink is place in the navigation
// bar instead of a navigation drop-down menu.
const NavigationLink = ({ description, path, topLevel, ...rest }) => (
  <LinkContainer key={path} to={path} {...rest}>
    {topLevel ? <NavItem>{description}</NavItem> : <MenuItem>{description}</MenuItem>}
  </LinkContainer>
);

NavigationLink.propTypes = {
  description: PropTypes.string.isRequired,
  path: PropTypes.string.isRequired,
  topLevel: PropTypes.bool,
};

NavigationLink.defaultProps = {
  topLevel: false,
};

export default NavigationLink;

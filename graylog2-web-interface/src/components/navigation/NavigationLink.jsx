import React from 'react';
import PropTypes from 'prop-types';
import { MenuItem } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

import URLUtils from 'util/URLUtils';

const NavigationLink = ({ description, path, ...rest }) => (
  <LinkContainer key={path} to={URLUtils.appPrefixed(path)} {...rest}>
    <MenuItem>{description}</MenuItem>
  </LinkContainer>
);

NavigationLink.propTypes = {
  description: PropTypes.string.isRequired,
  path: PropTypes.string.isRequired,
};

export default NavigationLink;

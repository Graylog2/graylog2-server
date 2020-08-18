import React from 'react';
import PropTypes from 'prop-types';

const Popover = ({ children }) => {
  return <>{children}</>;
};

Popover.propTypes = {
  children: PropTypes.any.isRequired,
};

export default Popover;

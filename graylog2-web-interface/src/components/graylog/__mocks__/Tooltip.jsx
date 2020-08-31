import React from 'react';
import PropTypes from 'prop-types';

const Tooltip = ({ children }) => {
  return <>{children}</>;
};

Tooltip.propTypes = {
  children: PropTypes.any.isRequired,
};

export default Tooltip;

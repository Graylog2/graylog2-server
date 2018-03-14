import React from 'react';
import PropTypes from 'prop-types';

const FullSizeContainer = ({ children }) => {
  return (
    <div style={{ height: '100%', width: '100%' }}>
      {children}
    </div>
  );
};

FullSizeContainer.propTypes = {
  children: PropTypes.element.isRequired,
};

export default FullSizeContainer;

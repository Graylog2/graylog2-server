import React from 'react';
import PropTypes from 'prop-types';

import GraylogThemeProvider from 'theme/GraylogThemeProvider';

const WrappingContainer = ({ children }) => {
  return (
    <GraylogThemeProvider>
      {children}
    </GraylogThemeProvider>
  );
};

WrappingContainer.propTypes = {
  children: PropTypes.node.isRequired,
};

export default WrappingContainer;

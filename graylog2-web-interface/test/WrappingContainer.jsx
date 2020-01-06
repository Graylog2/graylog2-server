import React from 'react';
import PropTypes from 'prop-types';

import GraylogThemeProvider from 'theme/GraylogThemeProvider';

/**
 * NOTE: `ThemeProvider` will be removed in future iterations and the actual global provider will be used
 */

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

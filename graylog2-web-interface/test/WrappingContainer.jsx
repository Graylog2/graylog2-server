import React from 'react';
import PropTypes from 'prop-types';
import { ThemeProvider } from 'styled-components';

/**
 * NOTE: `ThemeProvider` will be removed in future iterations and the actual global provider will be used
 */

const WrappingContainer = ({ children }) => {
  return (
    <ThemeProvider theme={{ mode: 'teinte' }}>
      {children}
    </ThemeProvider>
  );
};

WrappingContainer.propTypes = {
  children: PropTypes.node.isRequired,
};

export default WrappingContainer;

import React from 'react';
import PropTypes from 'prop-types';

import StyleguideThemeProvider from './StyleguideThemeProvider';

const StyleguideWrapper = ({ children }) => {
  return (
    <StyleguideThemeProvider>
      {children}
    </StyleguideThemeProvider>
  );
};

StyleguideWrapper.propTypes = {
  children: PropTypes.node.isRequired,
};

export default StyleguideWrapper;

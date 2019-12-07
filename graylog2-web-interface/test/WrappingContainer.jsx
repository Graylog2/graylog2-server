import React from 'react';
import PropTypes from 'prop-types';

import GraylogThemeProvider from 'theme/GraylogThemeProvider';
import GlobalThemeStyles from 'theme/GlobalThemeStyles';

const WrappingContainer = ({ children }) => {
  return (
    <GraylogThemeProvider>
      <>
        <GlobalThemeStyles />
        {children}
      </>
    </GraylogThemeProvider>
  );
};

WrappingContainer.propTypes = {
  children: PropTypes.node.isRequired,
};

export default WrappingContainer;

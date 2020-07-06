import React from 'react';
import PropTypes from 'prop-types';

import GlobalThemeStyles from 'theme/GlobalThemeStyles';

import AppGlobalNotifications from './AppGlobalNotifications';

const AppWithGlobalNotifications = ({ children }) => {
  return (
    <div>
      <GlobalThemeStyles />
      <AppGlobalNotifications />
      {children}
    </div>
  );
};

AppWithGlobalNotifications.propTypes = {
  children: PropTypes.oneOfType([
    PropTypes.arrayOf(PropTypes.element),
    PropTypes.element,
  ]).isRequired,
};

export default AppWithGlobalNotifications;

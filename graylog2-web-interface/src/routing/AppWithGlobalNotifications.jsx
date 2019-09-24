import React from 'react';
import PropTypes from 'prop-types';

import AppGlobalNotifications from './AppGlobalNotifications';
import AppWithScratchpad from './AppWithScratchpad';

const AppWithGlobalNotifications = ({ children }) => {
  return (
    <div>
      <AppGlobalNotifications />
      <AppWithScratchpad>
        {children}
      </AppWithScratchpad>
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

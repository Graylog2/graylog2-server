import React from 'react';
import PropTypes from 'prop-types';

import AppGlobalNotifications from './AppGlobalNotifications';

import Scratchpad from '../components/common/Scratchpad';

const AppWithGlobalNotifications = ({ children }) => {
  return (
    <div>
      <AppGlobalNotifications />
      {children}
      <Scratchpad />
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

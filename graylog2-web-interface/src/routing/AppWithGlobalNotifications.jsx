import React from 'react';
import PropTypes from 'prop-types';

import AppGlobalNotifications from './AppGlobalNotifications';

class AppWithGlobalNotifications extends React.Component {
  static propTypes = {
    children: PropTypes.oneOfType([
      PropTypes.arrayOf(PropTypes.element),
      PropTypes.element,
    ]).isRequired,
  };

  render() {
    return (
      <div>
        <AppGlobalNotifications />
        {this.props.children}
      </div>
    );
  }
}

export default AppWithGlobalNotifications;

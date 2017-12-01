import React from 'react';
import PropTypes from 'prop-types';

import AppGlobalNotifications from './AppGlobalNotifications';

const AppWithGlobalNotifications = React.createClass({
  propTypes: {
    children: PropTypes.oneOfType([
      PropTypes.arrayOf(PropTypes.element),
      PropTypes.element,
    ]).isRequired,
  },
  render() {
    return (
      <div>
        <AppGlobalNotifications />
        {this.props.children}
      </div>
    );
  },
});

export default AppWithGlobalNotifications;

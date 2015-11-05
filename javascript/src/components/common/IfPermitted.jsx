import React from 'react';
import Reflux from 'reflux';

import CurrentUserStore from 'stores/users/CurrentUserStore';

import PermissionsMixin from 'util/PermissionsMixin';

const IfPermitted = React.createClass({
  mixins: [Reflux.connect(CurrentUserStore), PermissionsMixin],
  propTypes: {
    permissions: React.PropTypes.oneOfType([
      React.PropTypes.string,
      React.PropTypes.arrayOf(React.PropTypes.string),
    ]).isRequired,
  },
  render() {
    const permissions = this.state.currentUser.permissions;
    if (this.isPermitted(permissions, this.props.permissions)) {
      return this.props.children;
    }

    return null;
  },
});

export default IfPermitted;

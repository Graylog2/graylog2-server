import React from 'react';
import Reflux from 'reflux';

import CurrentUserStore from 'stores/users/CurrentUserStore';

import PermissionsMixin from 'util/PermissionsMixin';

const IfPermitted = React.createClass({
  propTypes: {
    children: React.PropTypes.node.isRequired,
    permissions: React.PropTypes.oneOfType([
      React.PropTypes.string,
      React.PropTypes.arrayOf(React.PropTypes.string),
    ]).isRequired,
  },
  mixins: [Reflux.connect(CurrentUserStore), PermissionsMixin],
  render() {
    const permissions = this.state.currentUser.permissions;
    if (this.isPermitted(permissions, this.props.permissions)) {
      return React.Children.count(this.props.children) > 1 ? <span>{this.props.children}</span> : this.props.children;
    }

    return null;
  },
});

export default IfPermitted;

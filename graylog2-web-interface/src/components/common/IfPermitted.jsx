import React, { PropTypes } from 'react';
import Reflux from 'reflux';

import StoreProvider from 'injection/StoreProvider';
const CurrentUserStore = StoreProvider.getStore('CurrentUser');

import PermissionsMixin from 'util/PermissionsMixin';

const IfPermitted = React.createClass({
  propTypes: {
    children: PropTypes.node.isRequired,
    permissions: PropTypes.oneOfType([
      PropTypes.string,
      PropTypes.arrayOf(PropTypes.string),
    ]).isRequired,
    anyPermissions: PropTypes.bool,
  },
  mixins: [Reflux.connect(CurrentUserStore), PermissionsMixin],
  getDefaultProps() {
    return {
      anyPermissions: false,
    };
  },
  _checkPermissions() {
    if (this.props.anyPermissions) {
      return this.isAnyPermitted(this.state.currentUser.permissions, this.props.permissions);
    }

    return this.isPermitted(this.state.currentUser.permissions, this.props.permissions);
  },
  render() {
    if (this.state.currentUser && this._checkPermissions()) {
      return React.Children.count(this.props.children) > 1 ? <span>{this.props.children}</span> : this.props.children;
    }

    return null;
  },
});

export default IfPermitted;

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
  mixins: [Reflux.connect(CurrentUserStore, 'currentUser'), PermissionsMixin],
  getDefaultProps() {
    return {
      anyPermissions: false,
    };
  },
  _checkPermissions(currentUser) {
    if (this.props.anyPermissions) {
      return this.isAnyPermitted(currentUser.permissions, this.props.permissions);
    }

    return this.isPermitted(currentUser.permissions, this.props.permissions);
  },
  render() {
    const currentUser = this.state.currentUser;
    if (currentUser && this._checkPermissions(currentUser)) {
      return React.Children.count(this.props.children) > 1 ? <span>{this.props.children}</span> : this.props.children;
    }

    return null;
  },
});

export default IfPermitted;

import PropTypes from 'prop-types';
import React from 'react';
import { inject, observer } from 'mobx-react';

import StoreProvider from 'injection/StoreProvider';
const CurrentUserStore = StoreProvider.getStore('CurrentUser');

import PermissionsMixin from 'util/PermissionsMixin';

/**
 * Wrapper component that renders its children only if the current user fulfills certain permissions.
 * Current user's permissions are fetched from the server.
 */
const IfPermitted = React.createClass({
  propTypes: {
    /** Children to render if user has permissions. */
    children: PropTypes.node.isRequired,
    /** Current user logged into the system. */
    currentUser: PropTypes.object.isRequired,
    /** Permissions the current user must fulfill. By default, the user must have all permissions that are passed in this prop. */
    permissions: PropTypes.oneOfType([
      PropTypes.string,
      PropTypes.arrayOf(PropTypes.string),
    ]).isRequired,
    /** This flag controls which permissions the user must fulfill: (all, at least one). */
    anyPermissions: PropTypes.bool,
  },
  mixins: [PermissionsMixin],
  getDefaultProps() {
    return {
      anyPermissions: false,
    };
  },
  _checkPermissions() {
    if (this.props.anyPermissions) {
      return this.isAnyPermitted(this.props.currentUser.permissions, this.props.permissions);
    }

    return this.isPermitted(this.props.currentUser.permissions, this.props.permissions);
  },
  render() {
    if (this.props.currentUser && this._checkPermissions()) {
      return React.Children.count(this.props.children) > 1 ? <span>{this.props.children}</span> : this.props.children;
    }

    return null;
  },
});

export default inject(() => ({
  currentUser: CurrentUserStore.currentUser,
}))(observer(IfPermitted));

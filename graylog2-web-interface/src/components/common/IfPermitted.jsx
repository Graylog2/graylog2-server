import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';

import StoreProvider from 'injection/StoreProvider';
const CurrentUserStore = StoreProvider.getStore('CurrentUser');

import PermissionsMixin from 'util/PermissionsMixin';

/**
 * Wrapper component that renders its children only if the current user fulfills certain permissions.
 * Current user's permissions are fetched from the server.
 */
const IfPermitted = createReactClass({
  displayName: 'IfPermitted',

  propTypes: {
    /** Children to render if user has permissions. */
    children: PropTypes.node.isRequired,
    /** Permissions the current user must fulfill. By default, the user must have all permissions that are passed in this prop. */
    permissions: PropTypes.oneOfType([
      PropTypes.string,
      PropTypes.arrayOf(PropTypes.string),
    ]).isRequired,
    /** This flag controls which permissions the user must fulfill: (all, at least one). */
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

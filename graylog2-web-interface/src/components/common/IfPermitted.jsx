import PropTypes from 'prop-types';
import React from 'react';

import connect from 'stores/connect';
import StoreProvider from 'injection/StoreProvider';
import PermissionsMixin from 'util/PermissionsMixin';

const CurrentUserStore = StoreProvider.getStore('CurrentUser');
const { isPermitted, isAnyPermitted } = PermissionsMixin;

/**
 * Wrapper component that renders its children only if the current user fulfills certain permissions.
 * Current user's permissions are fetched from the server.
 */
const _checkPermissions = (permissions, anyPermissions, currentUser) => {
  if (anyPermissions) {
    return isAnyPermitted(currentUser.permissions, permissions);
  }

  return isPermitted(currentUser.permissions, permissions);
};

const IfPermitted = ({ children, currentUser, permissions, anyPermissions, ...rest }) => {
  if ((!permissions || permissions.length === 0) || (currentUser && _checkPermissions(permissions, anyPermissions, currentUser))) {
    return React.Children.map(children, (child) => {
      if (React.isValidElement(child)) {
        const presentProps = (child && child.props) ? Object.keys(child.props) : [];
        // do not overwrite existing props
        const filteredRest = Object.entries(rest)
          .filter(entry => !presentProps.includes(entry[0]))
          .reduce((obj, [k, v]) => ({ ...obj, [k]: v }), {});
        return React.cloneElement(child, filteredRest);
      }
      return child;
    });
  }

  return null;
};

IfPermitted.propTypes = {
  /** Children to render if user has permissions. */
  children: PropTypes.node.isRequired,
  /** Permissions the current user must fulfill. By default, the user must have all permissions that are passed in this prop. */
  permissions: PropTypes.oneOfType([
    PropTypes.string,
    PropTypes.arrayOf(PropTypes.string),
  ]).isRequired,
  /** This flag controls which permissions the user must fulfill: (all, at least one). */
  anyPermissions: PropTypes.bool,
};

IfPermitted.defaultProps = {
  anyPermissions: false,
};

/** @component */
export default connect(IfPermitted, { currentUser: CurrentUserStore }, ({ currentUser }) => ({ currentUser: currentUser ? currentUser.currentUser : currentUser }));

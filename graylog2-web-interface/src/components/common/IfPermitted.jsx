// @flow strict
import * as React from 'react';
import { useContext } from 'react';
import PropTypes from 'prop-types';

import CurrentUserContext from 'contexts/CurrentUserContext';
import { isPermitted, isAnyPermitted } from 'util/PermissionsMixin';

/**
 * Wrapper component that renders its children only if the current user fulfills certain permissions.
 * Current user's permissions are fetched from the server.
 */

type Props = {
  children: React.Node,
  permissions: string | Array<string>,
  anyPermissions?: boolean,
};

const _checkPermissions = (permissions, anyPermissions, currentUser) => {
  if (anyPermissions) {
    return isAnyPermitted(currentUser.permissions, permissions);
  }

  return isPermitted(currentUser.permissions, permissions);
};

const IfPermitted = ({ children, permissions, anyPermissions, ...rest }: Props) => {
  const currentUser = useContext(CurrentUserContext);

  if ((!permissions || permissions.length === 0) || (currentUser && _checkPermissions(permissions, anyPermissions, currentUser))) {
    return React.Children.map(children, (child) => {
      if (React.isValidElement(child)) {
        const presentProps = (child && child.props) ? Object.keys(child.props) : [];
        // do not overwrite existing props
        const filteredRest = Object.entries(rest)
          .filter((entry) => !presentProps.includes(entry[0]))
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
export default IfPermitted;

/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
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
  children: React.ReactElement | React.ReactElement[],
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
    return (
      <>
        {
          React.Children.map(children, (child) => {
            if (React.isValidElement(child)) {
              const presentProps = (child && child.props) ? Object.keys(child.props) : [];
              // do not overwrite existing props
              const filteredRest = Object.entries(rest)
                .filter((entry) => !presentProps.includes(entry[0]))
                .reduce((obj, [k, v]) => ({ ...obj, [k]: v }), {});

              return React.cloneElement(child, filteredRest);
            }

            return child;
          })
        }
      </>
    );
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

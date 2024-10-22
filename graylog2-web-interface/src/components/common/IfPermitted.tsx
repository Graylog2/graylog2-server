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
import * as React from 'react';

import useCurrentUser from 'hooks/useCurrentUser';
import { isPermitted, isAnyPermitted } from 'util/PermissionsMixin';

/**
 * Wrapper component that renders its children only if the current user fulfills certain permissions.
 * Current user's permissions are fetched from the server.
 */

type Props = {
  children: React.ReactNode,
  permissions: string | Array<string>,
  anyPermissions?: boolean,
};

const _checkPermissions = (permissions, anyPermissions, currentUser) => {
  if (anyPermissions) {
    return isAnyPermitted(currentUser.permissions, permissions);
  }

  return isPermitted(currentUser.permissions, permissions);
};

const IfPermitted = ({ children, permissions, anyPermissions = false, ...rest }: Props) => {
  const currentUser = useCurrentUser();

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

/** @component */
export default IfPermitted;

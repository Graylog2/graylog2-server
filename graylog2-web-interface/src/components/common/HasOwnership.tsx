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
import { createGRN } from 'logic/permissions/GRN';
import { UserJSON } from 'logic/users/User';

type ChildFun = ({ disabled: boolean }) => React.ReactElement;

type Props = {
  children: React.ReactElement | ChildFun,
  id: string,
  type: string,
  hideChildren: boolean,
};

const HasOwnership = ({ children, id, type, hideChildren }: Props) => {
  const currentUser = useContext<UserJSON>(CurrentUserContext);
  const entity = createGRN(type, id);
  const ownership = `entity:own:${entity}`;
  const adminPermission = '*';

  if (currentUser) {
    const { grn_permissions: grnPermissions = [], permissions } = currentUser;
    const isAdmin = permissions.includes(adminPermission);

    if (grnPermissions.includes(ownership) || isAdmin) {
      if (!hideChildren && typeof children === 'function') {
        return <>{ children({ disabled: false }) } </>;
      }

      return <>children</>;
    }
  }

  if (!hideChildren && typeof children === 'function') {
    return <>{ children({ disabled: true }) } </>;
  }

  return null;
};

HasOwnership.propTypes = {
  /** Children to render if user has ownership of the entity */
  children: PropTypes.oneOfType([
    PropTypes.node,
    PropTypes.func,
  ]).isRequired,
  /** The id string which shows entity */
  id: PropTypes.string,
  /** The type of the entity e.g dashboard, stream */
  type: PropTypes.string.isRequired,
  /** Will add disabled=true as a prop to the child in stead of not rendering it */
  hideChildren: PropTypes.bool,
};

HasOwnership.defaultProps = {
  hideChildren: false,
  id: undefined,
};

export default HasOwnership;

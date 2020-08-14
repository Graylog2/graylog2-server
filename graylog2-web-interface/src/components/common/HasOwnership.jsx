// @flow strict
import * as React from 'react';
import { useContext } from 'react';
import PropTypes from 'prop-types';

import CurrentUserContext from 'contexts/CurrentUserContext';
import { createGRN } from 'logic/permissions/GRN';

type Props = {
  children: React.Node,
  id: string,
  type: string,
};

const HasOwnership = ({ children, id, type }: Props) => {
  const currentUser = useContext(CurrentUserContext);
  const entity = createGRN(id, type);
  const ownership = `entity:own:${entity}`;
  const adminPermission = '*';

  if (currentUser) {
    const { grn_permissions: grnPermissions = [], permissions } = currentUser;
    const isAdmin = permissions.includes(adminPermission);

    if (grnPermissions.includes(ownership) || isAdmin) {
      return children;
    }
  }

  return null;
};

HasOwnership.propTypes = {
  /**  Children to render if user has entity of entity */
  children: PropTypes.node,
  /** The id string which shows entity */
  id: PropTypes.string,
  /** The type of the entity e.g dashboard, stream */
  type: PropTypes.string,
};

export default HasOwnership;
